package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.RpcException;
import burpmcp.rpc.RpcMethod;
import burpmcp.ui.ActiveEditorHelper;
import com.google.gson.JsonObject;

import javax.swing.JTextArea;
import java.util.HashMap;
import java.util.Map;

public class SetActiveEditorContents implements RpcMethod {
    private final MontoyaApi api;

    public SetActiveEditorContents(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "set_active_editor_contents";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        if (!params.has("text")) {
            throw new RpcException(RpcException.INVALID_PARAMS, "text parameter required");
        }
        String text = params.get("text").getAsString();

        JTextArea editor = ActiveEditorHelper.getActiveEditor(api);
        if (editor == null) {
            throw new RpcException(RpcException.INVALID_PARAMS, "No active editor");
        }
        if (!editor.isEditable()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "Current editor is not editable");
        }

        editor.setText(text);
        Map<String, Object> out = new HashMap<>();
        out.put("updated", true);
        out.put("length", text.length());
        return out;
    }
}
