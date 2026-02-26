package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class UrlDecode implements RpcMethod {
    private final MontoyaApi api;

    public UrlDecode(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "url_decode";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        if (!params.has("content")) {
            throw new RpcException(RpcException.INVALID_PARAMS, "content parameter required");
        }
        String content = params.get("content").getAsString();
        Map<String, Object> out = new HashMap<>();
        out.put("content", content);
        out.put("result", api.utilities().urlUtils().decode(content));
        return out;
    }
}
