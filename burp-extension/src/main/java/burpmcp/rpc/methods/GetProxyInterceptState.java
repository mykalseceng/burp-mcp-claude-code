package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class GetProxyInterceptState implements RpcMethod {
    private final MontoyaApi api;

    public GetProxyInterceptState(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_proxy_intercept_state";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        Map<String, Object> out = new HashMap<>();
        try {
            boolean intercepting = api.proxy().isInterceptEnabled();
            out.put("intercepting", intercepting);
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            throw new RpcException(
                RpcException.INTERNAL_ERROR,
                "proxy().isInterceptEnabled() not available in this Burp version"
            );
        }
        return out;
    }
}
