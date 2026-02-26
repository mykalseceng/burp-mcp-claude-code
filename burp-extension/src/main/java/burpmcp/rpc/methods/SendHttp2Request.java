package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpMode;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendHttp2Request implements RpcMethod {
    private final MontoyaApi api;

    public SendHttp2Request(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "send_http2_request";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String host = params.has("targetHostname") ? params.get("targetHostname").getAsString() : null;
        int port = params.has("targetPort") ? params.get("targetPort").getAsInt() : 443;
        boolean https = !params.has("usesHttps") || params.get("usesHttps").getAsBoolean();
        String body = params.has("requestBody") ? params.get("requestBody").getAsString() : "";

        if (host == null || host.isBlank()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "targetHostname parameter required");
        }

        List<HttpHeader> headers = new ArrayList<>();
        if (params.has("pseudoHeaders") && params.get("pseudoHeaders").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : params.getAsJsonObject("pseudoHeaders").entrySet()) {
                String key = entry.getKey().startsWith(":") ? entry.getKey() : ":" + entry.getKey();
                headers.add(HttpHeader.httpHeader(key.toLowerCase(), entry.getValue().getAsString()));
            }
        }
        if (params.has("headers") && params.get("headers").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : params.getAsJsonObject("headers").entrySet()) {
                headers.add(HttpHeader.httpHeader(entry.getKey().toLowerCase(), entry.getValue().getAsString()));
            }
        }

        HttpService service = HttpService.httpService(host, port, https);
        HttpRequest request = HttpRequest.http2Request(service, headers, body);
        var requestResponse = api.http().sendRequest(request, HttpMode.HTTP_2);

        Map<String, Object> out = new HashMap<>();
        out.put("targetHostname", host);
        out.put("targetPort", port);
        out.put("usesHttps", https);
        out.put("response", requestResponse == null ? "" : requestResponse.toString());
        out.put("statusCode", requestResponse != null && requestResponse.response() != null ? requestResponse.response().statusCode() : null);
        return out;
    }
}
