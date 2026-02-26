package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.RpcMethod;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class GenerateRandomString implements RpcMethod {
    private final MontoyaApi api;

    public GenerateRandomString(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "generate_random_string";
    }

    @Override
    public Object execute(JsonObject params) {
        int length = params.has("length") ? params.get("length").getAsInt() : 16;
        String characterSet = params.has("characterSet") ? params.get("characterSet").getAsString() : "ALPHANUMERIC";
        if (length < 1) {
            length = 1;
        }

        Map<String, Object> out = new HashMap<>();
        out.put("length", length);
        out.put("characterSet", characterSet);
        out.put("result", api.utilities().randomUtils().randomString(length, characterSet));
        return out;
    }
}
