package burpmcp.rpc;

public class RpcException extends Exception {
    private final int code;

    public RpcException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }

    // Standard error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int PRO_REQUIRED = -32001;
    public static final int NOT_IN_SCOPE = -32002;
    public static final int TIMEOUT = -32003;
}
