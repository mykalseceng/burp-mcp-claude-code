package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burp.api.montoya.scanner.Crawl;
import burp.api.montoya.scanner.CrawlConfiguration;
import burp.api.montoya.scanner.audit.Audit;
import burpmcp.jobs.JobManager;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StartScan implements RpcMethod {
    private final MontoyaApi api;
    private final JobManager jobManager;
    private static final Map<String, ScanTaskInfo> scanTasksByJobId = new ConcurrentHashMap<>();

    public StartScan(MontoyaApi api, JobManager jobManager) {
        this.api = api;
        this.jobManager = jobManager;
    }

    @Override
    public String getName() {
        return "start_scan";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        try {
            api.scanner();
        } catch (UnsupportedOperationException e) {
            throw new RpcException(RpcException.PRO_REQUIRED, "Active scanning requires Burp Suite Professional");
        }

        String url = params.has("url") ? params.get("url").getAsString() : null;
        if (url == null || url.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "url parameter required");
        }

        boolean doCrawl = params.has("crawl") && params.get("crawl").getAsBoolean();

        String jobId = jobManager.submit("scan", ctx -> {
            ctx.setStage("starting");
            ctx.setProgress(5);
            String currentJobId = String.valueOf(ctx.getDetail("jobId"));
            ctx.putDetail("targetUrl", url);
            ctx.putDetail("crawlEnabled", doCrawl);

            Crawl crawl = null;
            Audit audit;

            try {
                if (doCrawl) {
                    CrawlConfiguration crawlConfig = CrawlConfiguration.crawlConfiguration(url);
                    crawl = api.scanner().startCrawl(crawlConfig);
                    ctx.putDetail("crawlStarted", true);
                }

                HttpRequest request = HttpRequest.httpRequestFromUrl(url);
                AuditConfiguration auditConfig = AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS);
                audit = api.scanner().startAudit(auditConfig);
                audit.addRequest(request);

                scanTasksByJobId.put(currentJobId, new ScanTaskInfo(crawl, audit, url));

                ctx.setStage("running");
                ctx.setProgress(20);

                int idleLoops = 0;
                int lastActivityCount = -1;
                while (!ctx.isCancelled()) {
                    int crawlRequests = crawl == null ? 0 : crawl.requestCount();
                    int auditRequests = audit.requestCount();
                    int totalRequests = crawlRequests + auditRequests;

                    ctx.putDetail("crawlRequestCount", crawlRequests);
                    ctx.putDetail("auditRequestCount", auditRequests);
                    ctx.putDetail("errorCount", (crawl == null ? 0 : crawl.errorCount()) + audit.errorCount());

                    if (totalRequests == lastActivityCount) {
                        idleLoops++;
                    } else {
                        idleLoops = 0;
                    }
                    lastActivityCount = totalRequests;

                    ctx.putDetail("crawlStatus", "");
                    ctx.putDetail("auditStatus", "");

                    if (idleLoops >= 8) {
                        break;
                    }

                    int progress = Math.min(95, 20 + Math.max(1, totalRequests));
                    ctx.setProgress(progress);
                    Thread.sleep(1000);
                }

                if (ctx.isCancelled()) {
                    if (crawl != null) {
                        try { crawl.delete(); } catch (Exception ignored) {}
                    }
                    try { audit.delete(); } catch (Exception ignored) {}
                    throw new InterruptedException("scan job cancelled");
                }

                // Ensure crawl does not continue beyond this job lifecycle.
                if (crawl != null) {
                    try { crawl.delete(); } catch (Exception ignored) {}
                }

                int issueCount = audit.issues() == null ? 0 : audit.issues().size();
                ctx.putDetail("issueCount", issueCount);
                ctx.setProgress(100);

                Map<String, Object> out = new HashMap<>();
                out.put("targetUrl", url);
                out.put("crawlEnabled", doCrawl);
                out.put("issueCount", issueCount);
                out.put("message", "Scan completed for: " + url);
                return out;
            } finally {
                scanTasksByJobId.remove(currentJobId);
            }
        }, cancelJobId -> {
            ScanTaskInfo task = scanTasksByJobId.get(cancelJobId);
            if (task != null) {
                if (task.hasCrawl()) {
                    try { task.getCrawl().delete(); } catch (Exception ignored) {}
                }
                if (task.getAudit() != null) {
                    try { task.getAudit().delete(); } catch (Exception ignored) {}
                }
            }
            scanTasksByJobId.remove(cancelJobId);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("scanId", jobId);
        result.put("status", "queued");
        result.put("crawlEnabled", doCrawl);
        result.put("message", "Scan job queued for: " + url);
        return result;
    }

    public static void clearAllScanTasks() {
        scanTasksByJobId.clear();
    }

    public static class ScanTaskInfo {
        private final Crawl crawl;
        private final Audit audit;
        private final String targetUrl;

        public ScanTaskInfo(Crawl crawl, Audit audit, String targetUrl) {
            this.crawl = crawl;
            this.audit = audit;
            this.targetUrl = targetUrl;
        }

        public Crawl getCrawl() { return crawl; }
        public Audit getAudit() { return audit; }
        public String getTargetUrl() { return targetUrl; }
        public boolean hasCrawl() { return crawl != null; }
    }
}
