package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.replay.ReplayPackService;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import burpmcp.traffic.StoredRequest;
import burpmcp.traffic.TrafficStore;
import com.google.gson.JsonObject;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportReplayPack implements RpcMethod {
    private final MontoyaApi api;
    private final TrafficStore trafficStore;
    private final ReplayPackService replayPackService;

    public ExportReplayPack(MontoyaApi api, TrafficStore trafficStore, ReplayPackService replayPackService) {
        this.api = api;
        this.trafficStore = trafficStore;
        this.replayPackService = replayPackService;
    }

    @Override
    public String getName() {
        return "export_replay_pack";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String outputPath = params.has("outputPath") ? params.get("outputPath").getAsString() : null;
        if (outputPath == null || outputPath.isBlank()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "outputPath parameter required");
        }

        String domain = params.has("domain") ? params.get("domain").getAsString() : null;
        int limit = params.has("limit") ? params.get("limit").getAsInt() : 500;

        if (domain == null || domain.isBlank()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "domain parameter required");
        }

        List<StoredRequest> requests = trafficStore.getByDomain(domain, limit, null, null);
        List<Map<String, Object>> findings = new ArrayList<>();
        for (var issue : api.siteMap().issues()) {
            if (!issue.baseUrl().contains(domain)) {
                continue;
            }
            Map<String, Object> finding = new HashMap<>();
            finding.put("name", issue.name());
            finding.put("severity", issue.severity().name());
            finding.put("confidence", issue.confidence().name());
            finding.put("baseUrl", issue.baseUrl());
            finding.put("detail", issue.detail());
            findings.add(finding);
        }

        List<String> scopeUrls = new ArrayList<>();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("domain", domain);
        metadata.put("burpVersion", api.burpSuite().version().toString());
        metadata.put("burpEdition", api.burpSuite().version().edition().name());

        try {
            return replayPackService.exportPack(Paths.get(outputPath), metadata, requests, findings, scopeUrls);
        } catch (Exception e) {
            throw new RpcException(RpcException.INTERNAL_ERROR, "Failed to export replay pack: " + e.getMessage());
        }
    }
}
