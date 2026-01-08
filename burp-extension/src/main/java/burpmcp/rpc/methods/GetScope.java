package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class GetScope implements RpcMethod {
    private final MontoyaApi api;

    public GetScope(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_scope";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        // Check if a URL was provided to check scope status
        if (params.has("url")) {
            String url = params.get("url").getAsString();
            boolean inScope = api.scope().isInScope(url);

            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("inScope", inScope);
            return result;
        }

        // Otherwise return info about how to use this method
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Provide a 'url' parameter to check if it's in scope");
        result.put("usage", "Call with {url: 'https://example.com'} to check scope status");
        result.put("note", "Use modify_scope to add/remove URLs from scope");
        return result;
    }
}
