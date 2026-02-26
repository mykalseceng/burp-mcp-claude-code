package burpmcp.rpc.methods;

import burpmcp.jobs.JobManager;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class CancelJob implements RpcMethod {
    private final JobManager jobManager;

    public CancelJob(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public String getName() {
        return "cancel_job";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String jobId = params.has("jobId") ? params.get("jobId").getAsString() : null;
        if (jobId == null || jobId.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "jobId parameter required");
        }

        boolean cancelled = jobManager.cancel(jobId);
        if (!cancelled) {
            throw new RpcException(RpcException.INVALID_PARAMS, "No job found with ID: " + jobId);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("jobId", jobId);
        out.put("cancelled", true);
        return out;
    }
}
