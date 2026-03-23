package burpmcp.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private static final int MAX_STORED_EVENTS = 1000;

    private final List<EventListener> listeners;
    private final ConcurrentLinkedDeque<EventRecord> storedEvents;

    public EventBus() {
        this.listeners = new CopyOnWriteArrayList<>();
        this.storedEvents = new ConcurrentLinkedDeque<>();
    }

    public void register(EventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unregister(EventListener listener) {
        listeners.remove(listener);
    }

    public void publish(String type, Map<String, Object> payload) {
        EventRecord event = new EventRecord(type, payload);

        storedEvents.addFirst(event);
        while (storedEvents.size() > MAX_STORED_EVENTS) {
            storedEvents.pollLast();
        }

        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception ignored) {
            }
        }
    }

    public List<EventRecord> getRecentEvents(int limit) {
        List<EventRecord> result = new ArrayList<>();
        for (EventRecord event : storedEvents) {
            if (result.size() >= limit) {
                break;
            }
            result.add(event);
        }
        return result;
    }

    public int getTotalEventCount() {
        return storedEvents.size();
    }
}
