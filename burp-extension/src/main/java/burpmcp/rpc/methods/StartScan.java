package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burp.api.montoya.scanner.Crawl;
import burp.api.montoya.scanner.CrawlConfiguration;
import burp.api.montoya.scanner.audit.Audit;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StartScan implements RpcMethod {
    private final MontoyaApi api;

    // Store scan tasks for status checking
    private static final Map<String, ScanTaskInfo> activeScanTasks = new ConcurrentHashMap<>();

    public StartScan(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "start_scan";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        // Check if Scanner is available (Burp Pro only)
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

        String scanId = UUID.randomUUID().toString();
        Crawl crawl = null;
        Audit audit = null;

        // Start crawl if requested
        if (doCrawl) {
            CrawlConfiguration crawlConfig = CrawlConfiguration.crawlConfiguration(url);
            crawl = api.scanner().startCrawl(crawlConfig);
        }

        // Start audit
        HttpRequest request = HttpRequest.httpRequestFromUrl(url);
        AuditConfiguration auditConfig = AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS);
        audit = api.scanner().startAudit(auditConfig);
        audit.addRequest(request);

        // Store task references for future status queries
        activeScanTasks.put(scanId, new ScanTaskInfo(crawl, audit, url));

        Map<String, Object> result = new HashMap<>();
        result.put("scanId", scanId);
        result.put("status", "running");
        result.put("crawlEnabled", doCrawl);
        result.put("message", (doCrawl ? "Crawl and audit" : "Audit") + " started for: " + url);

        return result;
    }

    /**
     * Get stored scan task info by ID (for future status checking)
     */
    public static ScanTaskInfo getScanTask(String scanId) {
        return activeScanTasks.get(scanId);
    }

    /**
     * Get all active scan tasks
     */
    public static Map<String, ScanTaskInfo> getActiveScanTasks() {
        return activeScanTasks;
    }

    /**
     * Remove a scan task by ID (call after scan completes or is stopped)
     */
    public static void removeScanTask(String scanId) {
        activeScanTasks.remove(scanId);
    }

    /**
     * Clear all scan tasks (call on extension unload)
     */
    public static void clearAllScanTasks() {
        activeScanTasks.clear();
    }

    /**
     * Holds references to crawl and audit tasks for a scan
     */
    public static class ScanTaskInfo {
        private final Crawl crawl;
        private final Audit audit;
        private final String targetUrl;
        private final long startTime;

        public ScanTaskInfo(Crawl crawl, Audit audit, String targetUrl) {
            this.crawl = crawl;
            this.audit = audit;
            this.targetUrl = targetUrl;
            this.startTime = System.currentTimeMillis();
        }

        public Crawl getCrawl() { return crawl; }
        public Audit getAudit() { return audit; }
        public String getTargetUrl() { return targetUrl; }
        public long getStartTime() { return startTime; }
        public boolean hasCrawl() { return crawl != null; }
    }
}
