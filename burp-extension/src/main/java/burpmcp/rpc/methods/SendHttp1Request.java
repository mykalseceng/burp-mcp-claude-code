package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class SendHttp1Request implements RpcMethod {
    private final MontoyaApi api;

    public SendHttp1Request(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "send_http1_request";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String host = params.has("targetHostname") ? params.get("targetHostname").getAsString() : null;
        int port = params.has("targetPort") ? params.get("targetPort").getAsInt() : 443;
        boolean https = !params.has("usesHttps") || params.get("usesHttps").getAsBoolean();
        String content = params.has("content") ? params.get("content").getAsString() : null;

        if (host == null || host.isBlank()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "targetHostname parameter required");
        }
        if (content == null || content.isBlank()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "content parameter required");
        }

        HttpService service = HttpService.httpService(host, port, https);
        HttpRequest request = HttpRequest.httpRequest(service, content);
        var requestResponse = api.http().sendRequest(request);

        Map<String, Object> out = new HashMap<>();
        out.put("targetHostname", host);
        out.put("targetPort", port);
        out.put("usesHttps", https);
        out.put("response", requestResponse == null ? "" : requestResponse.toString());
        out.put("statusCode", requestResponse != null && requestResponse.response() != null ? requestResponse.response().statusCode() : null);
        return out;
    }
}
