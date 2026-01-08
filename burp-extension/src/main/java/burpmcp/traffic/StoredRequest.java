package burpmcp.traffic;

import java.util.Map;
import java.util.HashMap;

public class StoredRequest {
    private final long id;
    private final long timestamp;
    private final String method;
    private final String url;
    private final String host;
    private final int port;
    private final boolean isHttps;
    private final Map<String, String> requestHeaders;
    private final String requestBody;
    private final int statusCode;
    private final Map<String, String> responseHeaders;
    private final String responseBody;
    private final String mimeType;
    private final String toolSource;

    // Builder pattern constructor
    private StoredRequest(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.method = builder.method;
        this.url = builder.url;
        this.host = builder.host;
        this.port = builder.port;
        this.isHttps = builder.isHttps;
        this.requestHeaders = new HashMap<>(builder.requestHeaders);
        this.requestBody = builder.requestBody;
        this.statusCode = builder.statusCode;
        this.responseHeaders = new HashMap<>(builder.responseHeaders);
        this.responseBody = builder.responseBody;
        this.mimeType = builder.mimeType;
        this.toolSource = builder.toolSource;
    }

    // Getters
    public long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isHttps() { return isHttps; }
    public Map<String, String> getRequestHeaders() { return requestHeaders; }
    public String getRequestBody() { return requestBody; }
    public int getStatusCode() { return statusCode; }
    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public String getResponseBody() { return responseBody; }
    public String getMimeType() { return mimeType; }
    public String getToolSource() { return toolSource; }

    public static class Builder {
        private long id;
        private long timestamp = System.currentTimeMillis();
        private String method;
        private String url;
        private String host;
        private int port;
        private boolean isHttps;
        private Map<String, String> requestHeaders = new HashMap<>();
        private String requestBody = "";
        private int statusCode;
        private Map<String, String> responseHeaders = new HashMap<>();
        private String responseBody = "";
        private String mimeType = "";
        private String toolSource = "";

        public Builder id(long id) { this.id = id; return this; }
        public Builder timestamp(long ts) { this.timestamp = ts; return this; }
        public Builder method(String m) { this.method = m; return this; }
        public Builder url(String u) { this.url = u; return this; }
        public Builder host(String h) { this.host = h; return this; }
        public Builder port(int p) { this.port = p; return this; }
        public Builder isHttps(boolean https) { this.isHttps = https; return this; }
        public Builder requestHeaders(Map<String, String> h) { this.requestHeaders = h; return this; }
        public Builder requestBody(String b) { this.requestBody = b; return this; }
        public Builder statusCode(int s) { this.statusCode = s; return this; }
        public Builder responseHeaders(Map<String, String> h) { this.responseHeaders = h; return this; }
        public Builder responseBody(String b) { this.responseBody = b; return this; }
        public Builder mimeType(String m) { this.mimeType = m; return this; }
        public Builder toolSource(String t) { this.toolSource = t; return this; }

        public StoredRequest build() {
            return new StoredRequest(this);
        }
    }
}
