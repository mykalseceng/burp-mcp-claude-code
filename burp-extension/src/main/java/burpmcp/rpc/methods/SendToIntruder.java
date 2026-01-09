package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.HttpService;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class SendToIntruder implements RpcMethod {
    private final MontoyaApi api;

    public SendToIntruder(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "send_to_intruder";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String requestContent = params.has("request") ? params.get("request").getAsString() : null;
        String host = params.has("host") ? params.get("host").getAsString() : null;
        int port = params.has("port") ? params.get("port").getAsInt() : 443;
        boolean https = !params.has("https") || params.get("https").getAsBoolean();
        String tabName = params.has("tabName") ? params.get("tabName").getAsString() : null;

        if (requestContent == null || requestContent.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "request parameter required");
        }
        if (host == null || host.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "host parameter required");
        }

        // Normalize line endings (LF to CRLF for HTTP protocol)
        String normalizedRequest = requestContent.replace("\r\n", "\n").replace("\n", "\r\n");

        HttpService service = HttpService.httpService(host, port, https);
        HttpRequest request = HttpRequest.httpRequest(service, normalizedRequest);

        api.intruder().sendToIntruder(request, tabName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Request sent to Intruder" + (tabName != null ? " (tab: " + tabName + ")" : ""));
        result.put("host", host);
        result.put("port", port);
        result.put("https", https);

        return result;
    }
}
