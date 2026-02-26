package burpmcp.jobs;

@FunctionalInterface
public interface JobCanceller {
    void cancel(String jobId) throws Exception;
}
