package burpmcp.jobs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobContext {
    private final AtomicBoolean cancelled;
    private final ConcurrentHashMap<String, Object> details;
    private volatile int progress;
    private volatile String stage;

    JobContext(AtomicBoolean cancelled) {
        this.cancelled = cancelled;
        this.details = new ConcurrentHashMap<>();
        this.progress = 0;
        this.stage = "queued";
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void setProgress(int progress) {
        if (progress < 0) {
            this.progress = 0;
        } else {
            this.progress = Math.min(progress, 100);
        }
    }

    public int getProgress() {
        return progress;
    }

    public void setStage(String stage) {
        this.stage = stage == null ? "" : stage;
    }

    public String getStage() {
        return stage;
    }

    public void putDetail(String key, Object value) {
        if (key != null) {
            details.put(key, value);
        }
    }

    public Object getDetail(String key) {
        if (key == null) {
            return null;
        }
        return details.get(key);
    }

    public Map<String, Object> detailsSnapshot() {
        return Map.copyOf(details);
    }
}
