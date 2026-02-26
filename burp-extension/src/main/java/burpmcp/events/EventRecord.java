package burpmcp.events;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class EventRecord {
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private final String eventId;
    private final long timestamp;
    private final String type;
    private final Map<String, Object> payload;

    public EventRecord(String type, Map<String, Object> payload) {
        this.eventId = "evt_" + SEQUENCE.incrementAndGet();
        this.timestamp = Instant.now().toEpochMilli();
        this.type = type;
        this.payload = payload == null ? new HashMap<>() : new HashMap<>(payload);
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<>();
        out.put("eventId", eventId);
        out.put("timestamp", timestamp);
        out.put("type", type);
        out.put("payload", payload);
        return out;
    }
}
