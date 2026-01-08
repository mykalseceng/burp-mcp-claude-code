package burpmcp.rpc;

import com.google.gson.JsonObject;

public interface RpcMethod {
    String getName();
    Object execute(JsonObject params) throws RpcException;
}
