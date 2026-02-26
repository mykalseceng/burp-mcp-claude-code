package burpmcp.rpc.methods;

import burpmcp.jobs.JobManager;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListJobs implements RpcMethod {
    private final JobManager jobManager;

    public ListJobs(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public String getName() {
        return "list_jobs";
    }

    @Override
    public Object execute(JsonObject params) {
        List<Map<String, Object>> jobs = jobManager.list();
        Map<String, Object> out = new HashMap<>();
        out.put("jobs", jobs);
        out.put("total", jobs.size());
        return out;
    }
}
