package burpmcp.config;

import burp.api.montoya.persistence.Preferences;

public class ExtensionConfig {
    private static final String KEY_WS_PORT = "burpmcp.ws.port";
    private static final String KEY_AUTH_TOKEN = "burpmcp.auth.token";
    private static final String KEY_MAX_REQUESTS = "burpmcp.max.requests";
    private static final String KEY_MAX_BODY_SIZE = "burpmcp.max.body.size";

    private static final int DEFAULT_WS_PORT = 8198;
    private static final int DEFAULT_MAX_REQUESTS = 100;
    private static final int DEFAULT_MAX_BODY_SIZE = 102400; // 100KB

    private final Preferences prefs;

    public ExtensionConfig(Preferences prefs) {
        this.prefs = prefs;
    }

    public int getWebSocketPort() {
        Integer port = prefs.getInteger(KEY_WS_PORT);
        return port != null ? port : DEFAULT_WS_PORT;
    }

    public void setWebSocketPort(int port) {
        prefs.setInteger(KEY_WS_PORT, port);
    }

    public String getAuthToken() {
        String token = prefs.getString(KEY_AUTH_TOKEN);
        return token != null ? token : "";
    }

    public int getMaxRequestsPerDomain() {
        Integer max = prefs.getInteger(KEY_MAX_REQUESTS);
        return max != null ? max : DEFAULT_MAX_REQUESTS;
    }

    public int getMaxBodySize() {
        Integer max = prefs.getInteger(KEY_MAX_BODY_SIZE);
        return max != null ? max : DEFAULT_MAX_BODY_SIZE;
    }
}
