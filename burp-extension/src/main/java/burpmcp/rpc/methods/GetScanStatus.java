package burpmcp.rpc.methods;

import burpmcp.jobs.JobManager;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class GetScanStatus implements RpcMethod {
    private final JobManager jobManager;

    public GetScanStatus(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public String getName() {
        return "get_scan_status";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String jobId = null;
        if (params.has("jobId")) {
            jobId = params.get("jobId").getAsString();
        } else if (params.has("scanId")) {
            jobId = params.get("scanId").getAsString();
        }

        if (jobId == null || jobId.isBlank()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "jobId or scanId parameter required");
        }

        Map<String, Object> status = jobManager.status(jobId);
        if (status == null) {
            throw new RpcException(RpcException.INVALID_PARAMS, "No scan job found with ID: " + jobId);
        }

        Map<String, Object> out = new HashMap<>(status);
        out.put("scanId", jobId);
        return out;
    }
}
