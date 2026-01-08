package burpmcp.rpc.methods;

import burpmcp.rpc.*;
import burpmcp.traffic.TrafficStore;
import burpmcp.traffic.StoredRequest;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class GetProxyHistory implements RpcMethod {
    private final TrafficStore store;

    public GetProxyHistory(TrafficStore store) {
        this.store = store;
    }

    @Override
    public String getName() {
        return "get_proxy_history";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String domain = params.has("domain") ? params.get("domain").getAsString() : null;
        if (domain == null || domain.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "domain parameter required");
        }

        int limit = params.has("limit") ? params.get("limit").getAsInt() : 50;
        String method = params.has("method") ? params.get("method").getAsString() : null;
        Integer statusCode = params.has("statusCode") ? params.get("statusCode").getAsInt() : null;

        List<StoredRequest> requests = store.getByDomain(domain, limit, method, statusCode);
        int total = store.getTotalForDomain(domain);

        Map<String, Object> result = new HashMap<>();
        result.put("requests", requests);
        result.put("total", total);
        result.put("returned", requests.size());

        return result;
    }
}
