package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GetProxyWebsocketHistoryRegex implements RpcMethod {
    private final MontoyaApi api;

    public GetProxyWebsocketHistoryRegex(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_proxy_websocket_history_regex";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        if (!params.has("regex")) {
            throw new RpcException(RpcException.INVALID_PARAMS, "regex parameter required");
        }
        String regex = params.get("regex").getAsString();
        int limit = params.has("limit") ? params.get("limit").getAsInt() : 50;
        int offset = params.has("offset") ? params.get("offset").getAsInt() : 0;
        if (limit < 1) {
            limit = 1;
        }
        if (offset < 0) {
            offset = 0;
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (Exception e) {
            throw new RpcException(RpcException.INVALID_PARAMS, "invalid regex: " + e.getMessage());
        }

        List<?> history = api.proxy().webSocketHistory();
        List<Object> matched = new ArrayList<>();
        for (Object item : history) {
            if (pattern.matcher(item.toString()).find()) {
                matched.add(item);
            }
        }

        int total = matched.size();
        int end = Math.min(total, offset + limit);
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = offset; i < end; i++) {
            Object entry = matched.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("summary", entry.toString());
            items.add(m);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("regex", regex);
        out.put("total", total);
        out.put("returned", items.size());
        out.put("offset", offset);
        out.put("limit", limit);
        out.put("items", items);
        return out;
    }
}
