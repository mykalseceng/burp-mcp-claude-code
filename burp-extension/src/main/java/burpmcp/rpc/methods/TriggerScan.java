package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burp.api.montoya.scanner.audit.Audit;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TriggerScan implements RpcMethod {
    private final MontoyaApi api;

    public TriggerScan(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "trigger_scan";
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

        HttpRequest request = HttpRequest.httpRequestFromUrl(url);

        AuditConfiguration config = AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS);
        Audit audit = api.scanner().startAudit(config);
        audit.addRequest(request);

        String scanId = UUID.randomUUID().toString();

        Map<String, Object> result = new HashMap<>();
        result.put("scanId", scanId);
        result.put("status", "queued");
        result.put("message", "Scan started for: " + url);

        return result;
    }
}
