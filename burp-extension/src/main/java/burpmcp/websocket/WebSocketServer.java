package burpmcp.websocket;

import burpmcp.events.EventRecord;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import burp.api.montoya.logging.Logging;

import java.net.InetSocketAddress;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer {
    private final Logging logging;
    private final MessageHandler messageHandler;
    private final EventSubscriptionManager subscriptionManager;
    private final String authToken;

    public WebSocketServer(int port, Logging logging, MessageHandler handler, EventSubscriptionManager subscriptionManager, String authToken) {
        super(new InetSocketAddress("127.0.0.1", port));
        this.logging = logging;
        this.messageHandler = handler;
        this.subscriptionManager = subscriptionManager;
        this.authToken = authToken;
        setReuseAddr(true);
        setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!authToken.isEmpty()) {
            String clientToken = handshake.getFieldValue("Authorization");
            if (!("Bearer " + authToken).equals(clientToken)) {
                conn.close(4001, "Unauthorized");
                logging.logToOutput("WebSocket connection rejected: Invalid token");
                return;
            }
        }
        logging.logToOutput("WebSocket client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        subscriptionManager.removeConnection(conn);
        logging.logToOutput("WebSocket client disconnected: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String response = messageHandler.handleMessage(conn, message);
        conn.send(response);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logging.logToError("WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        logging.logToOutput("WebSocket server started on port " + getPort());
    }

    public void shutdown() {
        try {
            stop(1000);
            logging.logToOutput("WebSocket server stopped");
        } catch (InterruptedException e) {
            logging.logToError("Error stopping WebSocket server: " + e.getMessage());
        }
    }

    public void publishEvent(EventRecord event) {
        subscriptionManager.publish(event);
    }
}
