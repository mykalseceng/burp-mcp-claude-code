package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burpmcp.jobs.JobManager;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import burpmcp.traffic.StoredRequest;
import burpmcp.traffic.TrafficStore;
import burpmcp.util.JsonUtils;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartBulkExport implements RpcMethod {
    private final MontoyaApi api;
    private final JobManager jobManager;
    private final TrafficStore trafficStore;

    public StartBulkExport(MontoyaApi api, JobManager jobManager, TrafficStore trafficStore) {
        this.api = api;
        this.jobManager = jobManager;
        this.trafficStore = trafficStore;
    }

    @Override
    public String getName() {
        return "start_bulk_export";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String exportType = params.has("exportType") ? params.get("exportType").getAsString() : "proxy_history";
        String outputPath = params.has("outputPath") ? params.get("outputPath").getAsString() : null;
        String domain = params.has("domain") ? params.get("domain").getAsString() : null;
        int limit = params.has("limit") ? params.get("limit").getAsInt() : 500;

        if (outputPath == null || outputPath.isBlank()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "outputPath parameter required");
        }

        String jobId = jobManager.submit("export", ctx -> {
            ctx.setStage("preparing");
            ctx.setProgress(10);

            Map<String, Object> payload = new HashMap<>();
            payload.put("exportType", exportType);
            payload.put("generatedAt", Instant.now().toEpochMilli());

            if ("proxy_history".equals(exportType)) {
                if (domain == null || domain.isBlank()) {
                    throw new IllegalArgumentException("domain required for proxy_history export");
                }
                List<StoredRequest> requests = trafficStore.getByDomain(domain, limit, null, null);
                payload.put("domain", domain);
                payload.put("count", requests.size());
                payload.put("requests", requests);
                ctx.putDetail("records", requests.size());
            } else if ("scanner_issues".equals(exportType)) {
                if (api.burpSuite().version().edition() != BurpSuiteEdition.PROFESSIONAL) {
                    throw new IllegalStateException("scanner_issues export requires Burp Suite Professional");
                }
                List<Map<String, Object>> issues = new ArrayList<>();
                for (var issue : api.siteMap().issues()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", issue.name());
                    map.put("severity", issue.severity().name());
                    map.put("confidence", issue.confidence().name());
                    map.put("baseUrl", issue.baseUrl());
                    map.put("detail", issue.detail());
                    issues.add(map);
                }
                payload.put("count", issues.size());
                payload.put("issues", issues);
                ctx.putDetail("records", issues.size());
            } else {
                throw new IllegalArgumentException("Unsupported exportType: " + exportType);
            }

            ctx.setStage("writing");
            ctx.setProgress(70);
            Path path = Paths.get(outputPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, JsonUtils.toJson(payload), StandardCharsets.UTF_8);

            ctx.setProgress(100);
            Map<String, Object> result = new HashMap<>();
            result.put("outputPath", path.toString());
            result.put("exportType", exportType);
            result.put("records", payload.get("count"));
            return result;
        });

        Map<String, Object> out = new HashMap<>();
        out.put("jobId", jobId);
        out.put("status", "queued");
        out.put("exportType", exportType);
        out.put("outputPath", outputPath);
        return out;
    }
}
