package burpmcp.jobs;

@FunctionalInterface
public interface JobCanceller {
    void cancel() throws Exception;
}
