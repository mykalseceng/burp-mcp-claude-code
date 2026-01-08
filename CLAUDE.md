# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BurpMCP enables Claude Code CLI to orchestrate Burp Suite through an MCP (Model Context Protocol) server. It bridges Claude and Burp Suite for AI-powered analysis of HTTP traffic, sending requests, accessing sitemaps, and triggering security scans.

**Architecture:**
```
Claude Code CLI  ←(stdio)→  MCP Server (TypeScript)  ←(WebSocket)→  Burp Extension (Java)
```

## Build Commands

### Java Burp Extension
```bash
cd burp-extension
./gradlew build              # Build JAR → build/libs/burp-mcp-extension-1.0.0.jar
./gradlew clean build        # Clean rebuild
```
Requires Java 21+.

### TypeScript MCP Server
```bash
cd mcp-server
npm install                  # Install dependencies
npm run build               # Compile TypeScript → dist/
npm run dev                 # Run with tsx (development)
npm start                   # Run compiled version
```
Requires Node.js 18+.

### Testing WebSocket Connection
```bash
# Install wscat globally if needed
npm install -g wscat

# Connect and test RPC
wscat -c ws://localhost:8198
> {"jsonrpc":"2.0","id":"1","method":"get_proxy_history","params":{"domain":"example.com","limit":5}}
```

## Project Structure

```
burpmcp/
├── IMPLEMENTATION_PLAN.md    # 7-phase roadmap with complete code templates
├── burp-extension/           # Java Burp Suite extension
│   └── src/main/java/burpmcp/
│       ├── BurpMcpExtension.java    # Extension entry point
│       ├── config/                   # Configuration management
│       ├── traffic/                  # HTTP traffic capture & storage
│       ├── websocket/                # WebSocket server & message handling
│       ├── rpc/                      # JSON-RPC DTOs & method implementations
│       └── util/                     # JSON utilities, serialization
└── mcp-server/               # TypeScript MCP server
    └── src/
        ├── index.ts                  # Entry point
        ├── server.ts                 # MCP server setup
        ├── burp-client.ts            # WebSocket client to Burp
        ├── tools/                    # MCP tool implementations
        └── types/                    # TypeScript type definitions
```

## Architecture

### Communication Protocol
All communication uses JSON-RPC 2.0 between MCP Server and Burp Extension over WebSocket (default port 8198).

### RPC Methods
| Method | Purpose |
|--------|---------|
| `get_proxy_history` | Query HTTP traffic by domain |
| `get_sitemap` | Get discovered endpoints |
| `send_request` | Send custom HTTP request |
| `trigger_scan` | Start vulnerability scan (Burp Pro only) |
| `get_scope` | Get scope rules |
| `modify_scope` | Add/remove scope rules |

### Key Components

**TrafficStore (Java)** - Thread-safe circular buffer storing HTTP requests by domain using ConcurrentLinkedDeque with automatic eviction.

**WebSocketServer (Java)** - Manages client connections and routes JSON-RPC messages to RpcMethod handlers.

**BurpClient (TypeScript)** - WebSocket client with Promise-based RPC calls, timeout handling, and automatic reconnection.

### Design Patterns
- Builder Pattern for immutable `StoredRequest` objects
- Thread-safe collections (ConcurrentHashMap, ConcurrentLinkedDeque)
- Handler Pattern for traffic interception
- Zod schemas for TypeScript request validation

## Configuration

### Extension Config (Java)
Stored via Burp preferences:
- `burpmcp.ws.port` - WebSocket port (default: 8198)
- `burpmcp.auth.token` - Auth token (empty default)
- `burpmcp.max.requests` - Max requests per domain (default: 100)
- `burpmcp.max.body.size` - Max body size (default: 100KB)

### Claude Code Config
Add to `~/.claude.json`:
```json
{
  "mcpServers": {
    "burp": {
      "command": "node",
      "args": ["/path/to/burpmcp/mcp-server/dist/index.js"],
      "env": {
        "BURP_WS_URL": "ws://localhost:8198"
      }
    }
  }
}
```

## Implementation Status

**Phases 1-6: Complete** - Core functionality implemented.
**Phase 7: Ready** - Integration testing and polish.

See `IMPLEMENTATION_PLAN.md` for complete code templates and detailed implementation guides for each phase.

## Key Dependencies

### Java
- `net.portswigger.burp.extensions:montoya-api:2025.10`
- `com.google.code.gson:gson:2.10.1`
- `org.java-websocket:Java-WebSocket:1.5.6`

### TypeScript
- `@modelcontextprotocol/sdk:^1.0.0`
- `ws:^8.16.0`
- `zod:^3.23.0`

## Important Notes

- **Read IMPLEMENTATION_PLAN.md first** - contains complete code templates for each phase
- **Thread safety is critical** - Java traffic store must use concurrent collections
- **Body truncation** - Large HTTP bodies truncated at 100KB to prevent memory issues
- **Burp Pro required** for `trigger_scan` functionality
- **JSON-RPC error codes** - Standard codes: -32700 (parse), -32600 (invalid request), -32601 (method not found), -32602 (invalid params), -32603 (internal); Custom codes: -32001 (Pro required), -32002 (not in scope), -32003 (timeout)
