package burpmcp.jobs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class JobManager {
    private final ExecutorService executor;
    private final AtomicLong sequence;
    private final ConcurrentHashMap<String, JobEntry> jobs;

    public JobManager() {
        this.executor = Executors.newCachedThreadPool();
        this.sequence = new AtomicLong(0);
        this.jobs = new ConcurrentHashMap<>();
    }

    public String submit(String type, JobExecutor executorFn) {
        return submit(type, executorFn, null);
    }

    public String submit(String type, JobExecutor executorFn, JobCanceller canceller) {
        String safeType = type == null || type.isBlank() ? "generic" : type.toLowerCase();
        String jobId = "job_" + safeType + "_" + sequence.incrementAndGet();

        AtomicBoolean cancelled = new AtomicBoolean(false);
        JobContext context = new JobContext(cancelled);
        context.putDetail("jobId", jobId);
        JobEntry entry = new JobEntry(jobId, safeType, context, canceller);
        jobs.put(jobId, entry);

        Future<?> future = executor.submit(() -> runJob(entry, executorFn));
        entry.setFuture(future);

        return jobId;
    }

    private void runJob(JobEntry entry, JobExecutor executorFn) {
        entry.setState(JobState.RUNNING);
        entry.setStartedAt(Instant.now().toEpochMilli());
        entry.context.setStage("running");

        try {
            Object result = executorFn.run(entry.context);
            if (entry.cancelled.get()) {
                entry.setState(JobState.CANCELLED);
                entry.context.setStage("cancelled");
            } else {
                entry.setState(JobState.COMPLETED);
                entry.context.setProgress(100);
                entry.context.setStage("completed");
            }
            entry.setResult(result);
        } catch (Exception e) {
            if (entry.cancelled.get()) {
                entry.setState(JobState.CANCELLED);
                entry.context.setStage("cancelled");
            } else {
                entry.setState(JobState.FAILED);
                entry.context.setStage("failed");
                entry.setError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        } finally {
            entry.setFinishedAt(Instant.now().toEpochMilli());
        }
    }

    public Map<String, Object> status(String jobId) {
        JobEntry entry = jobs.get(jobId);
        if (entry == null) {
            return null;
        }
        return entry.toMap();
    }

    public List<Map<String, Object>> list() {
        List<JobEntry> entries = new ArrayList<>(jobs.values());
        entries.sort(Comparator.comparingLong(JobEntry::getCreatedAt));

        List<Map<String, Object>> out = new ArrayList<>(entries.size());
        for (JobEntry entry : entries) {
            out.add(entry.toMap());
        }
        return out;
    }

    public boolean cancel(String jobId) {
        JobEntry entry = jobs.get(jobId);
        if (entry == null) {
            return false;
        }
        entry.cancelled.set(true);
        entry.context.setStage("cancelling");

        if (entry.canceller != null) {
            try {
                entry.canceller.cancel(jobId);
            } catch (Exception ignored) {
            }
        }

        Future<?> future = entry.getFuture();
        if (future != null) {
            future.cancel(true);
        }

        entry.setState(JobState.CANCELLED);
        entry.setFinishedAt(Instant.now().toEpochMilli());
        entry.context.setStage("cancelled");
        return true;
    }

    public void shutdown() {
        executor.shutdownNow();
        jobs.clear();
    }

    private static final class JobEntry {
        private final String jobId;
        private final String type;
        private final long createdAt;
        private final JobContext context;
        private final JobCanceller canceller;
        private final AtomicBoolean cancelled;
        private volatile JobState state;
        private volatile long startedAt;
        private volatile long finishedAt;
        private volatile Object result;
        private volatile String error;
        private volatile Future<?> future;

        private JobEntry(String jobId, String type, JobContext context, JobCanceller canceller) {
            this.jobId = jobId;
            this.type = type;
            this.createdAt = Instant.now().toEpochMilli();
            this.context = context;
            this.canceller = canceller;
            this.cancelled = new AtomicBoolean(false);
            this.state = JobState.QUEUED;
            this.startedAt = 0;
            this.finishedAt = 0;
        }

        long getCreatedAt() {
            return createdAt;
        }

        void setState(JobState state) {
            this.state = state;
        }

        void setStartedAt(long startedAt) {
            this.startedAt = startedAt;
        }

        void setFinishedAt(long finishedAt) {
            this.finishedAt = finishedAt;
        }

        void setResult(Object result) {
            this.result = result;
        }

        void setError(String error) {
            this.error = error;
        }

        Future<?> getFuture() {
            return future;
        }

        void setFuture(Future<?> future) {
            this.future = future;
        }

        Map<String, Object> toMap() {
            Map<String, Object> out = new HashMap<>();
            out.put("jobId", jobId);
            out.put("type", type);
            out.put("state", state.name().toLowerCase());
            out.put("progress", context.getProgress());
            out.put("stage", context.getStage());
            out.put("createdAt", createdAt);
            out.put("startedAt", startedAt == 0 ? null : startedAt);
            out.put("finishedAt", finishedAt == 0 ? null : finishedAt);
            out.put("details", context.detailsSnapshot());
            if (result != null) {
                out.put("result", result);
            }
            if (error != null) {
                out.put("error", error);
            }
            return out;
        }
    }
}
