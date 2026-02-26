package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.scanner.Crawl;
import burp.api.montoya.scanner.CrawlConfiguration;
import burpmcp.jobs.JobManager;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StartCrawl implements RpcMethod {
    private final MontoyaApi api;
    private final JobManager jobManager;

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

        String jobId = jobManager.submit("crawl", ctx -> {
            ctx.setStage("starting");
            ctx.setProgress(5);

            Crawl crawl = api.scanner().startCrawl(CrawlConfiguration.crawlConfiguration(url));
            ctx.setStage("running");
            ctx.setProgress(15);

            int idle = 0;
            int lastCount = -1;
            while (!ctx.isCancelled()) {
                int count = crawl.requestCount();
                ctx.putDetail("requestCount", count);
                ctx.putDetail("errorCount", crawl.errorCount());
                String status = crawl.statusMessage() == null ? "" : crawl.statusMessage();
                ctx.putDetail("statusMessage", status);

                if (count == lastCount) {
                    idle++;
                } else {
                    idle = 0;
                }
                lastCount = count;

                String lower = status.toLowerCase(Locale.ROOT);
                if (lower.contains("complete") || lower.contains("finished") || idle >= 8) {
                    break;
                }

                ctx.setProgress(Math.min(95, 15 + count));
                Thread.sleep(1000);
            }

            if (ctx.isCancelled()) {
                try { crawl.delete(); } catch (Exception ignored) {}
                throw new InterruptedException("crawl cancelled");
            }

            ctx.setProgress(100);
            Map<String, Object> out = new HashMap<>();
            out.put("targetUrl", url);
            out.put("requestCount", crawl.requestCount());
            out.put("errorCount", crawl.errorCount());
            return out;
        });

        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("status", "queued");
        result.put("targetUrl", url);
        return result;
    }
}
