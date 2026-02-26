package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class SetProxyInterceptState implements RpcMethod {
    private final MontoyaApi api;

    public SetProxyInterceptState(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "set_proxy_intercept_state";
    }

    @Override
    public Object execute(JsonObject params) {
        boolean intercepting = params.has("intercepting") && params.get("intercepting").getAsBoolean();
        if (intercepting) {
            api.proxy().enableIntercept();
        } else {
            api.proxy().disableIntercept();
        }

        Map<String, Object> out = new HashMap<>();
        out.put("intercepting", intercepting);
        out.put("message", "Intercept has been " + (intercepting ? "enabled" : "disabled"));
        return out;
    }
}
