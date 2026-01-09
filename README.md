# BurpMCP

BurpMCP enables Claude Code CLI to orchestrate Burp Suite through an MCP (Model Context Protocol) server. It bridges Claude and Burp Suite for AI-powered analysis of HTTP traffic, sending requests, accessing sitemaps, and triggering security scans.

## Why This Architecture?

Claude Code cannot directly interact with Burp Suite for several reasons:

1. **No Native Burp API Access** - Burp Suite runs as a standalone Java application with its own extension API. Claude Code has no way to call Java methods or access Burp's internal state directly.

2. **Process Isolation** - Claude Code runs in its own process and communicates with the outside world through MCP (Model Context Protocol). It cannot spawn or control GUI applications like Burp Suite.

3. **Different Runtime Environments** - Claude Code's MCP servers run in Node.js, while Burp extensions must be written in Java/Kotlin. A bridge is needed to connect these two worlds.

The solution is a two-component bridge architecture that translates between Claude Code's MCP protocol and Burp Suite's extension API.

## Architecture

```
┌─────────────────┐      stdio       ┌─────────────────┐    WebSocket    ┌─────────────────┐
│                 │    (JSON-RPC)    │                 │   (JSON-RPC)    │                 │
│  Claude Code    │◄────────────────►│   MCP Server    │◄───────────────►│ Burp Extension  │
│     CLI         │                  │  (TypeScript)   │                 │     (Java)      │
│                 │                  │                 │                 │                 │
└─────────────────┘                  └─────────────────┘                 └─────────────────┘
                                            │                                    │
                                            │                                    │
                                     Translates MCP              Accesses Burp's Montoya API
                                     tool calls to               for proxy history, sitemap,
                                     WebSocket RPC               scanning, and HTTP requests
```

### Component Responsibilities

#### MCP Server (TypeScript)

The MCP server acts as a translator between Claude Code and Burp Suite:

- **Speaks MCP Protocol** - Communicates with Claude Code over stdio using the Model Context Protocol, exposing tools that Claude can invoke
- **WebSocket Client** - Maintains a persistent connection to the Burp extension, forwarding requests and receiving responses
- **Tool Definitions** - Defines the available tools (`get_proxy_history`, `send_request`, etc.) with their parameters and descriptions
- **Request/Response Mapping** - Converts MCP tool calls into JSON-RPC requests for Burp, and formats Burp's responses for Claude

#### Burp Extension (Java)

The Burp extension provides access to Burp Suite's capabilities:

- **WebSocket Server** - Listens for connections from the MCP server on a configurable port (default: 8198)
- **Montoya API Integration** - Uses Burp's official extension API to access proxy history, site maps, scanner, and HTTP request capabilities
- **Traffic Storage** - Maintains a thread-safe circular buffer of captured HTTP traffic, indexed by domain
- **RPC Method Handlers** - Implements handlers for each supported operation (get_proxy_history, send_request, start_scan, etc.)

### Communication Flow

1. **User asks Claude** to analyze traffic for a domain
2. **Claude Code** invokes the `get_proxy_history` MCP tool
3. **MCP Server** receives the tool call and sends a JSON-RPC request over WebSocket to Burp
4. **Burp Extension** queries its traffic store and returns matching requests/responses
5. **MCP Server** formats the response and returns it to Claude Code
6. **Claude** analyzes the traffic and responds to the user

## Prerequisites

- **Burp Suite** (Community or Professional)
- **Java 21+** (for building the extension)
- **Node.js 18+** (for the MCP server)

## Installation

### 1. Build the Burp Extension

```bash
cd burp-extension
./gradlew build
```

The JAR file will be created at `build/libs/burp-mcp-extension-1.0.0.jar`.

### 2. Load Extension in Burp Suite

1. Open Burp Suite
2. Go to **Extensions** > **Installed**
3. Click **Add**
4. Select the JAR file: `burp-extension/build/libs/burp-mcp-extension-1.0.0.jar`
5. Verify "BurpMCP" appears in the extensions list
6. Check the **Output** tab for "BurpMCP extension loaded"

### 3. Build the MCP Server

```bash
cd mcp-server
npm install
npm run build
```

### 4. Configure Claude Code

Run from the repository root:

```bash
claude mcp add burp -- node /absolute/path/to/burpmcp/mcp-server/dist/index.js
```

Or with the WebSocket URL explicitly set:

```bash
claude mcp add burp -e BURP_WS_URL=ws://localhost:8198 -- node /absolute/path/to/burpmcp/mcp-server/dist/index.js
```

Replace `/absolute/path/to/burpmcp` with the actual path to this repository.

To verify it was added:

```bash
claude mcp list
```

### 5. Restart Claude Code

After adding the MCP server, restart Claude Code to load it.

## Available Tools

Once configured, the following tools are available in Claude Code:

| Tool | Description |
|------|-------------|
| `get_proxy_history` | Query HTTP requests/responses captured by Burp proxy for a specific domain |
| `get_sitemap` | Get discovered endpoints from Burp's site map |
| `send_request` | Send a custom HTTP request through Burp Suite |
| `start_scan` | Start an active vulnerability scan (Burp Professional only) |
| `stop_scan` | Stop a running scan by ID (Burp Professional only) |
| `get_scanner_issues` | Get vulnerability findings from Burp's scanner (Burp Professional only) |
| `get_scope` | Get the current Burp Suite scope configuration |
| `modify_scope` | Add or remove a URL from Burp's scope |

## Usage Examples

### Analyze Captured Traffic

```
Get the last 20 requests for api.example.com and look for authentication issues
```

### Send Custom Requests

```
Send a POST request to https://api.example.com/login with JSON body {"username": "test", "password": "test123"}
```

### Explore Site Map

```
Show me the sitemap for target.com and identify API endpoints
```

### Manage Scope

```
Add https://example.com to the Burp scope
```

## Configuration Options

### Burp Extension Configuration

The extension stores settings via Burp preferences:

| Setting | Default | Description |
|---------|---------|-------------|
| `burpmcp.ws.port` | 8198 | WebSocket server port |
| `burpmcp.auth.token` | (empty) | Authentication token for WebSocket connections |
| `burpmcp.max.requests` | 100 | Maximum requests stored per domain |
| `burpmcp.max.body.size` | 102400 | Maximum body size in bytes (100KB) |

### MCP Server Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BURP_WS_URL` | `ws://localhost:8198` | WebSocket URL to connect to Burp extension |
| `BURP_AUTH_TOKEN` | (empty) | Authentication token (must match extension config) |
| `BURP_RECONNECT_DELAY` | 5000 | Reconnection delay in milliseconds |

## Troubleshooting

### Extension Not Loading

1. Verify Java 21+ is installed: `java -version`
2. Check Burp's **Errors** tab for stack traces
3. Ensure the JAR includes all dependencies (fat JAR)

### WebSocket Connection Failed

1. Verify the extension is loaded and running
2. Check that port 8198 is not in use: `lsof -i :8198`
3. Test the connection manually:
   ```bash
   npm install -g wscat
   wscat -c ws://localhost:8198
   ```

### MCP Server Not Appearing in Claude Code

1. Verify `~/.claude.json` syntax is valid JSON
2. Check the path to `index.js` is absolute and correct
3. Restart Claude Code completely
4. Check Claude Code logs for MCP connection errors

### Tools Return "Not Connected"

1. Ensure Burp Suite is running with the extension loaded
2. Check the extension Output tab for WebSocket server status
3. Verify `BURP_WS_URL` matches the configured port

### Scan Features Not Working

The `start_scan` tool requires Burp Suite Professional. On Community Edition, it will return an error indicating Pro is required.

## Development

### Running in Development Mode

```bash
# Terminal 1: Run MCP server with hot reload
cd mcp-server
npm run dev

# Terminal 2: Test WebSocket connection
wscat -c ws://localhost:8198
{"jsonrpc":"2.0","id":"1","method":"get_proxy_history","params":{"domain":"example.com"}}
```

### Project Structure

```
burpmcp/
├── burp-extension/           # Java Burp Suite extension
│   ├── build.gradle.kts      # Gradle build configuration
│   └── src/main/java/burpmcp/
│       ├── BurpMcpExtension.java    # Extension entry point
│       ├── config/                   # Configuration management
│       ├── traffic/                  # HTTP traffic capture & storage
│       ├── websocket/                # WebSocket server
│       ├── rpc/                      # JSON-RPC handling
│       └── util/                     # Utilities
├── mcp-server/               # TypeScript MCP server
│   ├── package.json
│   └── src/
│       ├── index.ts          # Entry point
│       ├── server.ts         # MCP server setup
│       ├── burp-client.ts    # WebSocket client
│       └── tools/            # MCP tool implementations
└── README.md                 # This file
```

## License

MIT
