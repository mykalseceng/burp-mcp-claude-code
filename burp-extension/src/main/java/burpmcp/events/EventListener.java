package burpmcp.events;

@FunctionalInterface
public interface EventListener {
    void onEvent(EventRecord event);
}
