package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.RpcMethod;
import burpmcp.ui.ActiveEditorHelper;
import com.google.gson.JsonObject;

import javax.swing.JTextArea;
import java.util.HashMap;
import java.util.Map;

public class GetActiveEditorContents implements RpcMethod {
    private final MontoyaApi api;

    public GetActiveEditorContents(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_active_editor_contents";
    }

    @Override
    public Object execute(JsonObject params) {
        JTextArea editor = ActiveEditorHelper.getActiveEditor(api);
        Map<String, Object> out = new HashMap<>();
        if (editor == null) {
            out.put("hasActiveEditor", false);
            out.put("text", "");
            out.put("message", "No active editor");
            return out;
        }
        out.put("hasActiveEditor", true);
        out.put("editable", editor.isEditable());
        out.put("text", editor.getText());
        return out;
    }
}
