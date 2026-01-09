package burpmcp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burpmcp.config.ExtensionConfig;
import burpmcp.traffic.TrafficStore;
import burpmcp.traffic.TrafficHttpHandler;
import burpmcp.websocket.WebSocketServer;
import burpmcp.websocket.MessageHandler;
import burpmcp.rpc.methods.*;

public class BurpMcpExtension implements BurpExtension {
    private MontoyaApi api;
    private ExtensionConfig config;
    private TrafficStore trafficStore;
    private WebSocketServer wsServer;
    private MessageHandler messageHandler;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("BurpMCP");

        this.config = new ExtensionConfig(api.persistence().preferences());

        api.logging().logToOutput("BurpMCP extension loaded");
        api.logging().logToOutput("WebSocket port: " + config.getWebSocketPort());

        // Phase 2: Register HTTP handler
        this.trafficStore = new TrafficStore(config.getMaxRequestsPerDomain());
        api.http().registerHttpHandler(new TrafficHttpHandler(api, trafficStore, config));
        api.logging().logToOutput("Traffic capture enabled (max " + config.getMaxRequestsPerDomain() + " requests per domain)");

        // Phase 3: Start WebSocket server
        this.messageHandler = new MessageHandler();

        // Phase 4: Register RPC methods
        messageHandler.registerMethod(new GetProxyHistory(trafficStore));
        messageHandler.registerMethod(new GetSitemap(api));
        messageHandler.registerMethod(new SendRequest(api));
        messageHandler.registerMethod(new StartScan(api));
        messageHandler.registerMethod(new StopScan(api));
        messageHandler.registerMethod(new GetScope(api));
        messageHandler.registerMethod(new ModifyScope(api));
        messageHandler.registerMethod(new GetScannerIssues(api));
        messageHandler.registerMethod(new SendToRepeater(api));
        api.logging().logToOutput("Registered 9 RPC methods");

        this.wsServer = new WebSocketServer(
            config.getWebSocketPort(),
            api.logging(),
            messageHandler,
            config.getAuthToken()
        );
        wsServer.start();

        api.extension().registerUnloadingHandler(this::cleanup);
    }

    private void cleanup() {
        api.logging().logToOutput("BurpMCP extension unloading...");
        // Phase 3: Stop WebSocket server
        if (wsServer != null) {
            wsServer.shutdown();
        }
    }

    // Getters for use by RPC methods in Phase 4
    public MontoyaApi getApi() {
        return api;
    }

    public TrafficStore getTrafficStore() {
        return trafficStore;
    }

    public ExtensionConfig getConfig() {
        return config;
    }
}
