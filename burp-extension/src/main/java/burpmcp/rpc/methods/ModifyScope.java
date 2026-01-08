package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ModifyScope implements RpcMethod {
    private final MontoyaApi api;

    public ModifyScope(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "modify_scope";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String action = params.has("action") ? params.get("action").getAsString() : null;
        String url = params.has("url") ? params.get("url").getAsString() : null;

        if (action == null || url == null) {
            throw new RpcException(RpcException.INVALID_PARAMS, "action and url parameters required");
        }

        boolean wasInScope = api.scope().isInScope(url);

        if ("add".equalsIgnoreCase(action)) {
            api.scope().includeInScope(url);
        } else if ("remove".equalsIgnoreCase(action)) {
            api.scope().excludeFromScope(url);
        } else {
            throw new RpcException(RpcException.INVALID_PARAMS, "action must be 'add' or 'remove'");
        }

        boolean isNowInScope = api.scope().isInScope(url);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("action", action);
        result.put("url", url);
        result.put("wasInScope", wasInScope);
        result.put("isNowInScope", isNowInScope);

        return result;
    }
}
