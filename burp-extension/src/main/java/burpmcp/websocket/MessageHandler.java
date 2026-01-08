package burpmcp.websocket;

import burpmcp.rpc.*;
import burpmcp.util.JsonUtils;
import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.HashMap;

public class MessageHandler {
    private final Map<String, RpcMethod> methods = new HashMap<>();

    public void registerMethod(RpcMethod method) {
        methods.put(method.getName(), method);
    }

    public String handleMessage(String message) {
        RpcRequest request;
        try {
            request = JsonUtils.fromJson(message, RpcRequest.class);
        } catch (JsonSyntaxException e) {
            return JsonUtils.toJson(RpcResponse.error(null, RpcException.PARSE_ERROR, "Parse error"));
        }

        if (!request.isValid()) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.INVALID_REQUEST, "Invalid request"));
        }

        RpcMethod method = methods.get(request.getMethod());
        if (method == null) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.METHOD_NOT_FOUND,
                "Method not found: " + request.getMethod()));
        }

        try {
            Object result = method.execute(request.getParams());
            return JsonUtils.toJson(RpcResponse.success(request.getId(), result));
        } catch (RpcException e) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.INTERNAL_ERROR, e.getMessage()));
        }
    }
}
