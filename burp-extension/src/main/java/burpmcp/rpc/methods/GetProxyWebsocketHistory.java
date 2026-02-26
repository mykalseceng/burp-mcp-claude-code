package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetProxyWebsocketHistory implements RpcMethod {
    private final MontoyaApi api;

    public GetProxyWebsocketHistory(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_proxy_websocket_history";
    }

    @Override
    public Object execute(JsonObject params) {
        int limit = params.has("limit") ? params.get("limit").getAsInt() : 50;
        int offset = params.has("offset") ? params.get("offset").getAsInt() : 0;
        if (limit < 1) {
            limit = 1;
        }
        if (offset < 0) {
            offset = 0;
        }

        List<?> history = api.proxy().webSocketHistory();
        int total = history.size();
        int end = Math.min(total, offset + limit);

        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = offset; i < end; i++) {
            Object entry = history.get(i);
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("summary", entry.toString());
            items.add(m);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("total", total);
        out.put("returned", items.size());
        out.put("offset", offset);
        out.put("limit", limit);
        out.put("items", items);
        return out;
    }
}
