package burpmcp.websocket;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventSubscriptionManagerTest {

    @Test
    void matchesWildcardAndPrefixPatterns() {
        assertTrue(EventSubscriptionManager.matchesFilter(Set.of("*"), "proxy.request.captured"));
        assertTrue(EventSubscriptionManager.matchesFilter(Set.of("proxy.*"), "proxy.request.captured"));
        assertTrue(EventSubscriptionManager.matchesFilter(Set.of("scanner.issue.created"), "scanner.issue.created"));
        assertFalse(EventSubscriptionManager.matchesFilter(Set.of("proxy.*"), "scanner.issue.created"));
        assertFalse(EventSubscriptionManager.matchesFilter(Set.of(), "proxy.request.captured"));
    }
}
