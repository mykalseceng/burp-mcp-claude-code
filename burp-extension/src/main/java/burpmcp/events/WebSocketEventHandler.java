package burpmcp.events;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.websocket.BinaryMessageAction;
import burp.api.montoya.websocket.TextMessageAction;
import burp.api.montoya.websocket.WebSocketCreated;
import burp.api.montoya.websocket.WebSocketCreatedHandler;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class WebSocketEventHandler implements WebSocketCreatedHandler {
    private final EventBus eventBus;

    public WebSocketEventHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void handleWebSocketCreated(WebSocketCreated webSocketCreated) {
        Map<String, Object> created = new HashMap<>();
        created.put("url", webSocketCreated.upgradeRequest().url());
        created.put("toolSource", webSocketCreated.toolSource().toolType().toString());
        eventBus.publish("websocket.created", created);

        webSocketCreated.webSocket().registerMessageHandler(new burp.api.montoya.websocket.MessageHandler() {
            @Override
            public TextMessageAction handleTextMessage(burp.api.montoya.websocket.TextMessage textMessage) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("url", webSocketCreated.upgradeRequest().url());
                payload.put("direction", textMessage.direction().name());
                payload.put("payload", truncate(textMessage.payload(), 400));
                eventBus.publish("websocket.message.captured", payload);
                return TextMessageAction.continueWith(textMessage);
            }

            @Override
            public BinaryMessageAction handleBinaryMessage(burp.api.montoya.websocket.BinaryMessage binaryMessage) {
                ByteArray bytes = binaryMessage.payload();
                String encoded = Base64.getEncoder().encodeToString(bytes.getBytes());
                Map<String, Object> payload = new HashMap<>();
                payload.put("url", webSocketCreated.upgradeRequest().url());
                payload.put("direction", binaryMessage.direction().name());
                payload.put("length", bytes.length());
                payload.put("payloadBase64Prefix", encoded.substring(0, Math.min(120, encoded.length())));
                eventBus.publish("websocket.message.captured", payload);
                return BinaryMessageAction.continueWith(binaryMessage);
            }
        });
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }
}
