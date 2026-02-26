package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.scanner.Crawl;
import burp.api.montoya.scanner.CrawlConfiguration;
import burpmcp.jobs.JobManager;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StartCrawl implements RpcMethod {
    private final MontoyaApi api;
    private final JobManager jobManager;
    private static final Map<String, Crawl> crawlTasksByJobId = new ConcurrentHashMap<>();

    public StartCrawl(MontoyaApi api, JobManager jobManager) {
        this.api = api;
        this.jobManager = jobManager;
    }

    @Override
    public String getName() {
        return "start_crawl";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        try {
            api.scanner();
        } catch (UnsupportedOperationException e) {
            throw new RpcException(RpcException.PRO_REQUIRED, "Crawl requires Burp Suite Professional");
        }

        String url = params.has("url") ? params.get("url").getAsString() : null;
        if (url == null || url.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "url parameter required");
        }

        int idleLimitTmp = params.has("idleLimitSeconds") ? params.get("idleLimitSeconds").getAsInt() : 1800;
        int maxRuntimeTmp = params.has("maxRuntimeSeconds") ? params.get("maxRuntimeSeconds").getAsInt() : 86400;
        if (idleLimitTmp < 10) {
            idleLimitTmp = 10;
        }
        // Allow maxRuntimeSeconds=0 to mean "no hard runtime limit".
        if (maxRuntimeTmp < 0) {
            maxRuntimeTmp = 0;
        }
        if (maxRuntimeTmp > 0 && maxRuntimeTmp < idleLimitTmp) {
            maxRuntimeTmp = idleLimitTmp;
        }
        final int idleLimitSeconds = idleLimitTmp;
        final int maxRuntimeSeconds = maxRuntimeTmp;

        String jobId = jobManager.submit("crawl", ctx -> {
            ctx.setStage("starting");
            ctx.setProgress(5);
            String currentJobId = String.valueOf(ctx.getDetail("jobId"));
            ctx.putDetail("idleLimitSeconds", idleLimitSeconds);
            ctx.putDetail("maxRuntimeSeconds", maxRuntimeSeconds);

            Crawl crawl = api.scanner().startCrawl(CrawlConfiguration.crawlConfiguration(url));
            crawlTasksByJobId.put(currentJobId, crawl);
            try {
                ctx.setStage("running");
                ctx.setProgress(15);

                int idle = 0;
                int lastCount = -1;
                int activeLoops = 0;
                while (!ctx.isCancelled()) {
                    int count = crawl.requestCount();
                    ctx.putDetail("requestCount", count);
                    ctx.putDetail("errorCount", crawl.errorCount());
                    ctx.putDetail("statusMessage", "");

                    if (count == lastCount) {
                        idle++;
                    } else {
                        idle = 0;
                    }
                    lastCount = count;
                    activeLoops++;

                    if (maxRuntimeSeconds > 0 && activeLoops >= maxRuntimeSeconds) {
                        break;
                    }

                    if (idle >= idleLimitSeconds) {
                        break;
                    }

                    ctx.setProgress(Math.min(95, 15 + count));
                    Thread.sleep(1000);
                }

                if (ctx.isCancelled()) {
                    try { crawl.delete(); } catch (Exception ignored) {}
                    throw new InterruptedException("crawl cancelled");
                }

                // Ensure no background crawl remains once we mark this job complete.
                try { crawl.delete(); } catch (Exception ignored) {}

                if (crawl.requestCount() == 0) {
                    throw new IllegalStateException(
                        "Crawl completed with zero requests after " + activeLoops + "s (idleLimit=" + idleLimitSeconds + "s, maxRuntime=" + maxRuntimeSeconds + "s)"
                    );
                }

                ctx.setProgress(100);
                Map<String, Object> out = new HashMap<>();
                out.put("targetUrl", url);
                out.put("requestCount", crawl.requestCount());
                out.put("errorCount", crawl.errorCount());
                return out;
            } finally {
                crawlTasksByJobId.remove(currentJobId);
            }
        }, cancelJobId -> {
            Crawl crawl = crawlTasksByJobId.get(cancelJobId);
            if (crawl != null) {
                try { crawl.delete(); } catch (Exception ignored) {}
            }
            crawlTasksByJobId.remove(cancelJobId);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("status", "queued");
        result.put("targetUrl", url);
        return result;
    }

    public static void clearAllCrawlTasks() {
        crawlTasksByJobId.clear();
    }
}
