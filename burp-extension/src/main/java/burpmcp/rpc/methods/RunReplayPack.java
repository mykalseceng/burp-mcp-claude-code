package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.replay.ReplayPackService;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.nio.file.Path;

public class RunReplayPack implements RpcMethod {
    private final MontoyaApi api;
    private final ReplayPackService replayPackService;

    public RunReplayPack(MontoyaApi api, ReplayPackService replayPackService) {
        this.api = api;
        this.replayPackService = replayPackService;
    }

    @Override
    public String getName() {
        return "run_replay_pack";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String inputPath = params.has("inputPath") ? params.get("inputPath").getAsString() : null;
        if (inputPath == null || inputPath.isBlank()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "inputPath parameter required");
        }

        try {
            return replayPackService.runPack(Path.of(inputPath), api);
        } catch (Exception e) {
            throw new RpcException(RpcException.INTERNAL_ERROR, "Failed to run replay pack: " + e.getMessage());
        }
    }
}
