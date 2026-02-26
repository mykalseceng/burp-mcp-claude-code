package burpmcp.jobs;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobManagerTest {

    @Test
    void submitCompletesAndProvidesStatus() throws Exception {
        JobManager manager = new JobManager();
        try {
            String jobId = manager.submit("scan", ctx -> {
                ctx.setStage("running");
                ctx.setProgress(50);
                ctx.putDetail("step", "mid");
                Thread.sleep(50);
                return Map.of("ok", true);
            });

            Thread.sleep(150);

            Map<String, Object> status = manager.status(jobId);
            assertNotNull(status);
            assertEquals("completed", status.get("state"));
            assertEquals(100, status.get("progress"));
            assertEquals(jobId, status.get("jobId"));
            assertNotNull(status.get("result"));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void cancelMarksJobCancelled() throws Exception {
        JobManager manager = new JobManager();
        try {
            String jobId = manager.submit("crawl", ctx -> {
                while (!ctx.isCancelled()) {
                    Thread.sleep(25);
                }
                throw new InterruptedException("cancelled");
            });

            Thread.sleep(50);
            boolean cancelled = manager.cancel(jobId);
            assertTrue(cancelled);

            Thread.sleep(50);
            Map<String, Object> status = manager.status(jobId);
            assertEquals("cancelled", status.get("state"));
        } finally {
            manager.shutdown();
        }
    }
}
