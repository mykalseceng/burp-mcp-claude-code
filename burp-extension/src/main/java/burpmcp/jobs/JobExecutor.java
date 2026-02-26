package burpmcp.jobs;

@FunctionalInterface
public interface JobExecutor {
    Object run(JobContext context) throws Exception;
}
