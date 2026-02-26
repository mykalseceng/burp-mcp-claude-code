package burpmcp.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private final List<EventListener> listeners;

    public EventBus() {
        this.listeners = new CopyOnWriteArrayList<>();
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
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception ignored) {
            }
        }
    }
}
