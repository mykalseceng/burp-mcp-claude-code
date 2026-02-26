package burpmcp.rpc.methods;

import burpmcp.jobs.JobManager;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.Map;

public class GetJobStatus implements RpcMethod {
    private final JobManager jobManager;

    public GetJobStatus(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public String getName() {
        return "get_job_status";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String jobId = params.has("jobId") ? params.get("jobId").getAsString() : null;
        if (jobId == null || jobId.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "jobId parameter required");
        }

        Map<String, Object> status = jobManager.status(jobId);
        if (status == null) {
            throw new RpcException(RpcException.INVALID_PARAMS, "No job found with ID: " + jobId);
        }
        return status;
    }
}
