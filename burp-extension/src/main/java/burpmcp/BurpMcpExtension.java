package burpmcp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burpmcp.config.ExtensionConfig;
import burpmcp.events.EventBus;
import burpmcp.events.ScannerIssueEventHandler;
import burpmcp.events.WebSocketEventHandler;
import burpmcp.jobs.JobManager;
import burpmcp.replay.ReplayPackService;
import burpmcp.traffic.TrafficStore;
import burpmcp.traffic.TrafficHttpHandler;
import burpmcp.websocket.EventSubscriptionManager;
import burpmcp.websocket.WebSocketServer;
import burpmcp.websocket.MessageHandler;
import burpmcp.rpc.methods.*;

public class BurpMcpExtension implements BurpExtension {
    private MontoyaApi api;
    private ExtensionConfig config;
    private TrafficStore trafficStore;
    private JobManager jobManager;
    private EventBus eventBus;
    private EventSubscriptionManager eventSubscriptionManager;
    private ReplayPackService replayPackService;
    private WebSocketServer wsServer;
    private MessageHandler messageHandler;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("BurpMCP");

        this.config = new ExtensionConfig(api.persistence().preferences());

        api.logging().logToOutput("BurpMCP extension loaded");
        api.logging().logToOutput("WebSocket port: " + config.getWebSocketPort());

        this.trafficStore = new TrafficStore(config.getMaxRequestsPerDomain());
        this.jobManager = new JobManager();
        this.eventBus = new EventBus();
        this.eventSubscriptionManager = new EventSubscriptionManager();
        this.replayPackService = new ReplayPackService();

        api.http().registerHttpHandler(new TrafficHttpHandler(api, trafficStore, config, eventBus));
        api.logging().logToOutput("Traffic capture enabled (max " + config.getMaxRequestsPerDomain() + " requests per domain)");

        try {
            api.scanner().registerAuditIssueHandler(new ScannerIssueEventHandler(eventBus));
            api.logging().logToOutput("Scanner issue event streaming enabled");
        } catch (UnsupportedOperationException e) {
            api.logging().logToOutput("Scanner issue events unavailable in this Burp edition");
        }

        try {
            api.websockets().registerWebSocketCreatedHandler(new WebSocketEventHandler(eventBus));
            api.logging().logToOutput("WebSocket message event streaming enabled");
        } catch (UnsupportedOperationException e) {
            api.logging().logToOutput("WebSocket events unavailable in this Burp edition");
        }

        this.messageHandler = new MessageHandler(eventSubscriptionManager);
        messageHandler.registerMethod(new GetProxyHistory(trafficStore));
        messageHandler.registerMethod(new GetSitemap(api));
        messageHandler.registerMethod(new SendRequest(api));
        messageHandler.registerMethod(new StartScan(api, jobManager));
        messageHandler.registerMethod(new GetScanStatus(jobManager));
        messageHandler.registerMethod(new StopScan(jobManager));
        messageHandler.registerMethod(new StartCrawl(api, jobManager));
        messageHandler.registerMethod(new StartBulkExport(api, jobManager, trafficStore));
        messageHandler.registerMethod(new GetJobStatus(jobManager));
        messageHandler.registerMethod(new ListJobs(jobManager));
        messageHandler.registerMethod(new CancelJob(jobManager));
        messageHandler.registerMethod(new GetScope(api));
        messageHandler.registerMethod(new ModifyScope(api));
        messageHandler.registerMethod(new GetScannerIssues(api));
        messageHandler.registerMethod(new SendToRepeater(api));
        messageHandler.registerMethod(new SendToIntruder(api));
        messageHandler.registerMethod(new GetCapabilities(api));
        messageHandler.registerMethod(new ExportReplayPack(api, trafficStore, replayPackService));
        messageHandler.registerMethod(new RunReplayPack(api, replayPackService));
        api.logging().logToOutput("Registered 19 RPC methods (+3 event subscription RPCs)");

        this.wsServer = new WebSocketServer(
            config.getWebSocketPort(),
            api.logging(),
            messageHandler,
            eventSubscriptionManager,
            config.getAuthToken()
        );

        eventBus.register(wsServer::publishEvent);
        wsServer.start();

        api.extension().registerUnloadingHandler(this::cleanup);
    }

    private void cleanup() {
        api.logging().logToOutput("BurpMCP extension unloading...");
        if (wsServer != null) {
            wsServer.shutdown();
        }
        // Clear stored data to release memory
        if (trafficStore != null) {
            trafficStore.clearAll();
        }
        if (jobManager != null) {
            jobManager.shutdown();
        }
        StartScan.clearAllScanTasks();
        api.logging().logToOutput("BurpMCP cleanup complete");
    }
}
