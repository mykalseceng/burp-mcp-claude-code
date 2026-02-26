package burpmcp.events;

import burp.api.montoya.scanner.audit.AuditIssueHandler;
import burp.api.montoya.scanner.audit.issues.AuditIssue;

import java.util.HashMap;
import java.util.Map;

public class ScannerIssueEventHandler implements AuditIssueHandler {
    private final EventBus eventBus;

    public ScannerIssueEventHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void handleNewAuditIssue(AuditIssue auditIssue) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", auditIssue.name());
        payload.put("severity", auditIssue.severity().name());
        payload.put("confidence", auditIssue.confidence().name());
        payload.put("baseUrl", auditIssue.baseUrl());
        payload.put("detail", auditIssue.detail());
        eventBus.publish("scanner.issue.created", payload);
    }
}
