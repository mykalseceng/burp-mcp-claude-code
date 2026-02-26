package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Version;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class GetCapabilities implements RpcMethod {
    private final MontoyaApi api;

    public GetCapabilities(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_capabilities";
    }

    @Override
    public Object execute(JsonObject params) {
        Version version = api.burpSuite().version();

        Map<String, Object> burp = new HashMap<>();
        burp.put("name", version.name());
        burp.put("version", version.toString());
        burp.put("buildNumber", version.buildNumber());
        burp.put("edition", version.edition().name());

        Map<String, Object> features = new HashMap<>();
        features.put("scanner", probeCapability(() -> api.scanner()));
        features.put("collaborator", probeCapability(() -> api.collaborator()));
        features.put("ai", probeCapability(() -> api.ai()));
        features.put("websockets", probeCapability(() -> api.websockets()));
        features.put("proxy", probeCapability(() -> api.proxy()));
        features.put("siteMap", probeCapability(() -> api.siteMap()));
        features.put("scope", probeCapability(() -> api.scope()));
        features.put("repeater", probeCapability(() -> api.repeater()));
        features.put("intruder", probeCapability(() -> api.intruder()));

        Map<String, Object> result = new HashMap<>();
        result.put("rpcVersion", "1.0");
        result.put("burp", burp);
        result.put("features", features);

        return result;
    }

    private Map<String, Object> probeCapability(CapabilityProbe probe) {
        Map<String, Object> capability = new HashMap<>();
        try {
            probe.run();
            capability.put("available", true);
        } catch (UnsupportedOperationException e) {
            capability.put("available", false);
            capability.put("reason", e.getMessage() != null ? e.getMessage() : "unsupported in current Burp edition/configuration");
        } catch (RuntimeException e) {
            capability.put("available", false);
            capability.put("reason", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        return capability;
    }

    @FunctionalInterface
    private interface CapabilityProbe {
        void run();
    }
}
