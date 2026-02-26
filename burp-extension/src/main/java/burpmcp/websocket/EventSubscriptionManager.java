package burpmcp.websocket;

import burpmcp.events.EventRecord;
import burpmcp.util.JsonUtils;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EventSubscriptionManager {
    private final ConcurrentHashMap<WebSocket, Set<String>> subscriptions;

    public EventSubscriptionManager() {
        this.subscriptions = new ConcurrentHashMap<>();
    }

    public Map<String, Object> subscribe(WebSocket conn, List<String> eventTypes) {
        Set<String> set = subscriptions.computeIfAbsent(conn, c -> ConcurrentHashMap.newKeySet());
        if (eventTypes == null || eventTypes.isEmpty()) {
            set.add("*");
        } else {
            for (String eventType : eventTypes) {
                if (eventType != null && !eventType.isBlank()) {
                    set.add(eventType);
                }
            }
        }
        return status(conn);
    }

    public Map<String, Object> unsubscribe(WebSocket conn, List<String> eventTypes) {
        Set<String> set = subscriptions.get(conn);
        if (set == null) {
            return status(conn);
        }
        if (eventTypes == null || eventTypes.isEmpty()) {
            set.clear();
        } else {
            for (String eventType : eventTypes) {
                set.remove(eventType);
            }
        }
        if (set.isEmpty()) {
            subscriptions.remove(conn);
        }
        return status(conn);
    }

    public Map<String, Object> status(WebSocket conn) {
        Set<String> set = subscriptions.getOrDefault(conn, Set.of());
        List<String> types = new ArrayList<>(set);
        types.sort(String::compareTo);

        Map<String, Object> out = new HashMap<>();
        out.put("subscribed", !types.isEmpty());
        out.put("eventTypes", types);
        out.put("connectionId", connectionId(conn));
        return out;
    }

    public void removeConnection(WebSocket conn) {
        subscriptions.remove(conn);
    }

    public void publish(EventRecord event) {
        for (Map.Entry<WebSocket, Set<String>> entry : subscriptions.entrySet()) {
            WebSocket conn = entry.getKey();
            if (!conn.isOpen()) {
                subscriptions.remove(conn);
                continue;
            }

            Set<String> filter = entry.getValue();
            if (!matchesFilter(filter, event.getType())) {
                continue;
            }

            Map<String, Object> notification = new HashMap<>();
            notification.put("jsonrpc", "2.0");
            notification.put("method", "event");
            notification.put("params", event.toMap());
            conn.send(JsonUtils.toJson(notification));
        }
    }

    static boolean matchesFilter(Set<String> filter, String type) {
        if (filter == null || filter.isEmpty()) {
            return false;
        }
        if (filter.contains("*")) {
            return true;
        }
        if (type == null) {
            return false;
        }
        if (filter.contains(type)) {
            return true;
        }
        for (String item : filter) {
            if (item.endsWith("*") && type.startsWith(item.substring(0, item.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    private String connectionId(WebSocket conn) {
        if (conn == null || conn.getRemoteSocketAddress() == null) {
            return "unknown";
        }
        return conn.getRemoteSocketAddress().toString();
    }
}
