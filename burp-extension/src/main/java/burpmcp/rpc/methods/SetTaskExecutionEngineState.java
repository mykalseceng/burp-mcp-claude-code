package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static burp.api.montoya.burpsuite.TaskExecutionEngine.TaskExecutionEngineState.PAUSED;
import static burp.api.montoya.burpsuite.TaskExecutionEngine.TaskExecutionEngineState.RUNNING;

public class SetTaskExecutionEngineState implements RpcMethod {
    private final MontoyaApi api;

    public SetTaskExecutionEngineState(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "set_task_execution_engine_state";
    }

    @Override
    public Object execute(JsonObject params) {
        boolean running = !params.has("running") || params.get("running").getAsBoolean();
        api.burpSuite().taskExecutionEngine().setState(running ? RUNNING : PAUSED);

        Map<String, Object> out = new HashMap<>();
        out.put("running", running);
        out.put("message", "Task execution engine is now " + (running ? "running" : "paused"));
        return out;
    }
}
