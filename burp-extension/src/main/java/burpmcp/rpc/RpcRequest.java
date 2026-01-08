package burpmcp.rpc;

import com.google.gson.JsonObject;

public class RpcRequest {
    private String jsonrpc;
    private String id;
    private String method;
    private JsonObject params;

    public boolean isValid() {
        return "2.0".equals(jsonrpc) && id != null && method != null;
    }

    public String getJsonrpc() { return jsonrpc; }
    public String getId() { return id; }
    public String getMethod() { return method; }
    public JsonObject getParams() { return params != null ? params : new JsonObject(); }
}
