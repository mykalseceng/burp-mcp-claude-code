package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpHeader;
import burpmcp.rpc.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendRequest implements RpcMethod {
    private final MontoyaApi api;

    public SendRequest(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "send_request";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String url = params.has("url") ? params.get("url").getAsString() : null;
        if (url == null || url.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "url parameter required");
        }

        String method = params.has("method") ? params.get("method").getAsString() : "GET";
        String body = params.has("body") ? params.get("body").getAsString() : "";
        boolean addToSiteMap = params.has("addToSiteMap") && params.get("addToSiteMap").getAsBoolean();
        String source = params.has("source") ? params.get("source").getAsString() : "Claude Code";

        HttpRequest request = HttpRequest.httpRequestFromUrl(url).withMethod(method);

        if (params.has("headers")) {
            JsonObject headers = params.getAsJsonObject("headers");
            for (Map.Entry<String, JsonElement> entry : headers.entrySet()) {
                request = request.withHeader(entry.getKey(), entry.getValue().getAsString());
            }
        }

        if (!body.isEmpty()) {
            request = request.withBody(body);
        }

        long startTime = System.currentTimeMillis();
        HttpRequestResponse response = api.http().sendRequest(request);
        long duration = System.currentTimeMillis() - startTime;

        if (response.response() == null) {
            throw new RpcException(RpcException.INTERNAL_ERROR, "Request failed: no response received");
        }

        if (addToSiteMap) {
            Annotations annotations = Annotations.annotations(
                "Source: " + source,
                HighlightColor.CYAN
            );
            HttpRequestResponse annotatedResponse = response.withAnnotations(annotations);
            api.siteMap().add(annotatedResponse);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.response().statusCode());
        result.put("headers", headersToMap(response.response().headers()));
        result.put("body", response.response().bodyToString());
        result.put("time", duration);
        result.put("addedToSiteMap", addToSiteMap);

        return result;
    }

    private Map<String, String> headersToMap(List<HttpHeader> headers) {
        Map<String, String> map = new HashMap<>();
        for (HttpHeader h : headers) {
            map.put(h.name(), h.value());
        }
        return map;
    }
}
