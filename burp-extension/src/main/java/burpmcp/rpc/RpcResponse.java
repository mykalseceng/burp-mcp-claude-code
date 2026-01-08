package burpmcp.rpc;

public class RpcResponse {
    private final String jsonrpc = "2.0";
    private final String id;
    private Object result;
    private RpcError error;

    private RpcResponse(String id) {
        this.id = id;
    }

    public static RpcResponse success(String id, Object result) {
        RpcResponse response = new RpcResponse(id);
        response.result = result;
        return response;
    }

    public static RpcResponse error(String id, int code, String message) {
        return error(id, code, message, null);
    }

    public static RpcResponse error(String id, int code, String message, Object data) {
        RpcResponse response = new RpcResponse(id);
        response.error = new RpcError(code, message, data);
        return response;
    }

    public static class RpcError {
        private final int code;
        private final String message;
        private final Object data;

        RpcError(int code, String message, Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
    }
}
