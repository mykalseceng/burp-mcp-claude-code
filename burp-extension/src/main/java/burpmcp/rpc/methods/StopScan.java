package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class StopScan implements RpcMethod {
    private final MontoyaApi api;

    public StopScan(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "stop_scan";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String scanId = params.has("scanId") ? params.get("scanId").getAsString() : null;
        if (scanId == null || scanId.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "scanId parameter required");
        }

        StartScan.ScanTaskInfo taskInfo = StartScan.getScanTask(scanId);
        if (taskInfo == null) {
            throw new RpcException(RpcException.INVALID_PARAMS, "No scan found with ID: " + scanId);
        }

        boolean crawlStopped = false;
        boolean auditStopped = false;

        // Stop crawl if present
        if (taskInfo.hasCrawl() && taskInfo.getCrawl() != null) {
            try {
                taskInfo.getCrawl().delete();
                crawlStopped = true;
            } catch (Exception e) {
                // Crawl may have already completed or been deleted
            }
        }

        // Stop audit
        if (taskInfo.getAudit() != null) {
            try {
                taskInfo.getAudit().delete();
                auditStopped = true;
            } catch (Exception e) {
                // Audit may have already completed or been deleted
            }
        }

        // Remove from active scans
        StartScan.getActiveScanTasks().remove(scanId);

        Map<String, Object> result = new HashMap<>();
        result.put("scanId", scanId);
        result.put("stopped", true);
        result.put("crawlStopped", crawlStopped);
        result.put("auditStopped", auditStopped);
        result.put("targetUrl", taskInfo.getTargetUrl());
        result.put("message", "Scan stopped for: " + taskInfo.getTargetUrl());

        return result;
    }
}
