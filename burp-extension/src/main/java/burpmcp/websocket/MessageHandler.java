package burpmcp.websocket;

import burpmcp.rpc.*;
import burpmcp.util.JsonUtils;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class MessageHandler {
    private final Map<String, RpcMethod> methods = new HashMap<>();
    private final EventSubscriptionManager subscriptionManager;

    public MessageHandler(EventSubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    public void registerMethod(RpcMethod method) {
        methods.put(method.getName(), method);
    }

    public String handleMessage(WebSocket conn, String message) {
        RpcRequest request;
        try {
            request = JsonUtils.fromJson(message, RpcRequest.class);
        } catch (JsonSyntaxException e) {
            return JsonUtils.toJson(RpcResponse.error(null, RpcException.PARSE_ERROR, "Parse error"));
        }

        if (!request.isValid()) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.INVALID_REQUEST, "Invalid request"));
        }

        if ("subscribe_events".equals(request.getMethod())) {
            List<String> types = eventTypesFromParams(request.getParams());
            Object result = subscriptionManager.subscribe(conn, types);
            return JsonUtils.toJson(RpcResponse.success(request.getId(), result));
        }

        if ("unsubscribe_events".equals(request.getMethod())) {
            List<String> types = eventTypesFromParams(request.getParams());
            Object result = subscriptionManager.unsubscribe(conn, types);
            return JsonUtils.toJson(RpcResponse.success(request.getId(), result));
        }

        if ("get_event_subscriptions".equals(request.getMethod())) {
            Object result = subscriptionManager.status(conn);
            return JsonUtils.toJson(RpcResponse.success(request.getId(), result));
        }

        RpcMethod method = methods.get(request.getMethod());
        if (method == null) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.METHOD_NOT_FOUND,
                "Method not found: " + request.getMethod()));
        }

        try {
            Object result = method.execute(request.getParams(), new RpcContext(connectionId(conn)));
            return JsonUtils.toJson(RpcResponse.success(request.getId(), result));
        } catch (RpcException e) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.INTERNAL_ERROR, e.getMessage()));
        }
    }

    private List<String> eventTypesFromParams(com.google.gson.JsonObject params) {
        List<String> types = new ArrayList<>();
        if (!params.has("eventTypes") || !params.get("eventTypes").isJsonArray()) {
            return types;
        }
        for (var item : params.getAsJsonArray("eventTypes")) {
            if (item != null && item.isJsonPrimitive()) {
                types.add(item.getAsString());
            }
        }
        return types;
    }

    private String connectionId(WebSocket conn) {
        if (conn == null || conn.getRemoteSocketAddress() == null) {
            return "unknown";
        }
        return conn.getRemoteSocketAddress().toString();
    }
}
