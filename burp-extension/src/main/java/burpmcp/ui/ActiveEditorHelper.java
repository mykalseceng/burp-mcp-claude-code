package burpmcp.ui;

import burp.api.montoya.MontoyaApi;

import javax.swing.JTextArea;
import java.awt.KeyboardFocusManager;

public final class ActiveEditorHelper {
    private ActiveEditorHelper() {}

    public static JTextArea getActiveEditor(MontoyaApi api) {
        var frame = api.userInterface().swingUtils().suiteFrame();
        var focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        var permanentFocusOwner = focusManager.getPermanentFocusOwner();

        boolean isInBurpWindow = false;
        var component = permanentFocusOwner;
        while (component != null) {
            if (component == frame) {
                isInBurpWindow = true;
                break;
            }
            component = component.getParent();
        }

        if (isInBurpWindow && permanentFocusOwner instanceof JTextArea textArea) {
            return textArea;
        }

        return null;
    }
}
