package burpmcp.rpc;

import com.google.gson.JsonObject;

public interface RpcMethod {
    String getName();
    Object execute(JsonObject params) throws RpcException;

    default Object execute(JsonObject params, RpcContext context) throws RpcException {
        return execute(params);
    }
}
