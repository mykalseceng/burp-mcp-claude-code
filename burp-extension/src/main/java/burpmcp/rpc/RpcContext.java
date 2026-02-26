package burpmcp.rpc;

public class RpcContext {
    private final String connectionId;

    public RpcContext(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionId() {
        return connectionId;
    }
}
