package burpmcp.rpc.methods;

import burpmcp.rpc.*;
import burpmcp.traffic.TrafficStore;
import burpmcp.traffic.StoredRequest;
import com.google.gson.JsonObject;

public class GetProxyHistoryItem implements RpcMethod {
    private final TrafficStore store;

    public GetProxyHistoryItem(TrafficStore store) {
        this.store = store;
    }

    @Override
    public String getName() {
        return "get_proxy_history_item";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        if (!params.has("id")) {
            throw new RpcException(RpcException.INVALID_PARAMS, "id parameter required");
        }

        long id = params.get("id").getAsLong();
        StoredRequest request = store.getById(id);

        if (request == null) {
            throw new RpcException(RpcException.INVALID_PARAMS, "No request found with id " + id);
        }

        return request;
    }
}
