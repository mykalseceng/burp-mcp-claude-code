package burpmcp.rpc.methods;

import burpmcp.rpc.*;
import burpmcp.events.EventBus;
import burpmcp.events.EventRecord;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class GetEventLog implements RpcMethod {
    private final EventBus eventBus;

    public GetEventLog(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getName() {
        return "get_event_log";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        int limit = params.has("limit") ? params.get("limit").getAsInt() : 50;
        String level = params.has("level") ? params.get("level").getAsString() : null;
        String search = params.has("search") ? params.get("search").getAsString() : null;

        List<EventRecord> events = eventBus.getRecentEvents(limit);

        List<EventRecord> filtered = events.stream()
            .filter(e -> level == null || e.getType().startsWith(level))
            .filter(e -> search == null || payloadContains(e, search))
            .collect(Collectors.toList());

        int total = eventBus.getTotalEventCount();

        List<Map<String, Object>> eventMaps = new ArrayList<>();
        for (EventRecord e : filtered) {
            eventMaps.add(e.toMap());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("events", eventMaps);
        result.put("total", total);
        result.put("returned", eventMaps.size());

        return result;
    }

    private boolean payloadContains(EventRecord event, String search) {
        Map<String, Object> map = event.toMap();
        Object payload = map.get("payload");
        return payload != null && payload.toString().toLowerCase().contains(search.toLowerCase());
    }
}
