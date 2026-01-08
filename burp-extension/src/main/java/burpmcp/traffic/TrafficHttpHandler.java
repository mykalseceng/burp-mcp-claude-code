package burpmcp.traffic;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burpmcp.config.ExtensionConfig;

import java.util.Map;
import java.util.stream.Collectors;

public class TrafficHttpHandler implements HttpHandler {
    private final MontoyaApi api;
    private final TrafficStore store;
    private final ExtensionConfig config;

    public TrafficHttpHandler(MontoyaApi api, TrafficStore store, ExtensionConfig config) {
        this.api = api;
        this.store = store;
        this.config = config;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
        return RequestToBeSentAction.continueWith(request);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
        try {
            HttpRequest request = response.initiatingRequest();

            StoredRequest.Builder builder = new StoredRequest.Builder()
                .timestamp(System.currentTimeMillis())
                .method(request.method())
                .url(request.url())
                .host(request.httpService().host())
                .port(request.httpService().port())
                .isHttps(request.httpService().secure())
                .requestHeaders(headersToMap(request.headers()))
                .requestBody(truncateBody(request.bodyToString()))
                .statusCode(response.statusCode())
                .responseHeaders(headersToMap(response.headers()))
                .responseBody(truncateBody(response.bodyToString()))
                .mimeType(response.mimeType().toString())
                .toolSource(response.toolSource().toolType().toString());

            store.store(builder);

        } catch (Exception e) {
            api.logging().logToError("Error storing request: " + e.getMessage());
        }

        return ResponseReceivedAction.continueWith(response);
    }

    private Map<String, String> headersToMap(java.util.List<HttpHeader> headers) {
        return headers.stream()
            .collect(Collectors.toMap(
                HttpHeader::name,
                HttpHeader::value,
                (v1, v2) -> v1 + ", " + v2
            ));
    }

    private String truncateBody(String body) {
        int maxSize = config.getMaxBodySize();
        if (body == null) return "";
        if (body.length() <= maxSize) return body;
        return body.substring(0, maxSize) + "\n[TRUNCATED - " + body.length() + " bytes total]";
    }
}
