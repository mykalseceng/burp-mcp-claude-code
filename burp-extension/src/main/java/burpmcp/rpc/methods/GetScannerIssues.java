package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetScannerIssues implements RpcMethod {
    private final MontoyaApi api;

    public GetScannerIssues(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_scanner_issues";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        // Check if running Burp Professional
        if (api.burpSuite().version().edition() != BurpSuiteEdition.PROFESSIONAL) {
            throw new RpcException(RpcException.PRO_REQUIRED, "Scanner issues require Burp Suite Professional");
        }

        int limit = params.has("limit") ? params.get("limit").getAsInt() : 100;
        int offset = params.has("offset") ? params.get("offset").getAsInt() : 0;
        String urlFilter = params.has("url") ? params.get("url").getAsString() : null;
        String severityFilter = params.has("severity") ? params.get("severity").getAsString().toUpperCase() : null;

        List<AuditIssue> allIssues = api.siteMap().issues();
        List<Map<String, Object>> issues = new ArrayList<>();

        int count = 0;
        int skipped = 0;

        for (AuditIssue issue : allIssues) {
            // Apply URL filter if specified
            if (urlFilter != null && !issue.baseUrl().contains(urlFilter)) {
                continue;
            }

            // Apply severity filter if specified
            if (severityFilter != null && !issue.severity().name().equals(severityFilter)) {
                continue;
            }

            // Handle offset
            if (skipped < offset) {
                skipped++;
                continue;
            }

            // Check limit
            if (count >= limit) {
                break;
            }

            issues.add(serializeIssue(issue));
            count++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("issues", issues);
        result.put("total", allIssues.size());
        result.put("returned", issues.size());
        result.put("offset", offset);

        return result;
    }

    private Map<String, Object> serializeIssue(AuditIssue issue) {
        Map<String, Object> map = new HashMap<>();

        map.put("name", issue.name());
        map.put("severity", issue.severity().name());
        map.put("confidence", issue.confidence().name());
        map.put("baseUrl", issue.baseUrl());
        map.put("detail", issue.detail());
        map.put("remediation", issue.remediation());

        // HTTP Service info
        if (issue.httpService() != null) {
            Map<String, Object> service = new HashMap<>();
            service.put("host", issue.httpService().host());
            service.put("port", issue.httpService().port());
            service.put("secure", issue.httpService().secure());
            map.put("httpService", service);
        }

        // Issue definition
        if (issue.definition() != null) {
            Map<String, Object> definition = new HashMap<>();
            definition.put("name", issue.definition().name());
            definition.put("background", issue.definition().background());
            definition.put("remediation", issue.definition().remediation());
            definition.put("typeIndex", issue.definition().typeIndex());
            map.put("definition", definition);
        }

        // Request/Response pairs (limit to avoid huge payloads)
        List<Map<String, String>> requestResponses = new ArrayList<>();
        int maxPairs = 3; // Limit request/response pairs
        int pairCount = 0;
        for (var reqResp : issue.requestResponses()) {
            if (pairCount >= maxPairs) break;

            Map<String, String> pair = new HashMap<>();
            pair.put("request", reqResp.request() != null ? truncate(reqResp.request().toString(), 5000) : null);
            pair.put("response", reqResp.response() != null ? truncate(reqResp.response().toString(), 5000) : null);
            requestResponses.add(pair);
            pairCount++;
        }
        map.put("requestResponses", requestResponses);

        // Collaborator interactions
        List<Map<String, String>> interactions = new ArrayList<>();
        for (var interaction : issue.collaboratorInteractions()) {
            Map<String, String> inter = new HashMap<>();
            inter.put("id", interaction.id().toString());
            inter.put("timestamp", interaction.timeStamp().toString());
            interactions.add(inter);
        }
        map.put("collaboratorInteractions", interactions);

        return map;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "... (truncated)";
    }
}
