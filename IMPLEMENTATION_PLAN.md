# Burp Suite MCP Integration - Multi-Agent Implementation Plan

## Overview

Build a system that allows Claude Code CLI to orchestrate Burp Suite through an MCP server, enabling capabilities like analyzing HTTP traffic, sending requests, accessing the sitemap, and triggering scans.

## Architecture

```
┌─────────────────┐     stdio      ┌─────────────────┐   WebSocket   ┌─────────────────┐
│   Claude Code   │◄──────────────►│   MCP Server    │◄─────────────►│ Burp Extension  │
│      CLI        │                │  (TypeScript)   │   JSON-RPC    │     (Java)      │
└─────────────────┘                └─────────────────┘               └─────────────────┘
```

---

## Reference Materials (IMPORTANT)

**All agents MUST consult these reference materials before implementing their assigned phase.**

### Montoya API Source (Official Burp Extension API)
**Location:** `../Dev/burp-extensions-montoya-api/`

Key files to reference:
- `src/main/java/burp/api/montoya/BurpExtension.java` - Extension entry point interface
- `src/main/java/burp/api/montoya/MontoyaApi.java` - Main API interface with all sub-APIs
- `src/main/java/burp/api/montoya/http/Http.java` - HTTP operations (sendRequest, registerHttpHandler)
- `src/main/java/burp/api/montoya/http/handler/HttpHandler.java` - Traffic interception interface
- `src/main/java/burp/api/montoya/http/message/requests/HttpRequest.java` - Request building/modification
- `src/main/java/burp/api/montoya/http/message/responses/HttpResponse.java` - Response access
- `src/main/java/burp/api/montoya/sitemap/SiteMap.java` - Sitemap access
- `src/main/java/burp/api/montoya/scope/Scope.java` - Scope management
- `src/main/java/burp/api/montoya/scanner/Scanner.java` - Active scanning (Pro only)
- `src/main/java/burp/api/montoya/persistence/Persistence.java` - Settings storage
- `src/main/java/burp/api/montoya/logging/Logging.java` - Extension logging

### Official Montoya API Examples
**Location:** `../Dev/burp-extensions-montoya-api-examples/`

| Example | Location | Use For |
|---------|----------|---------|
| **helloworld** | `helloworld/src/main/java/example/helloworld/HelloWorld.java` | Extension entry point pattern, logging |
| **httphandler** | `httphandler/src/main/java/example/httphandler/` | HTTP traffic capture (Phase 2) |
| **proxyhandler** | `proxyhandler/src/main/java/example/proxyhandler/` | Proxy-specific handling |
| **customlogger** | `customlogger/src/main/java/example/customlogger/` | UI tabs, request/response storage pattern |
| **contextmenu** | `contextmenu/src/main/java/example/contextmenu/` | Context menu integration |
| **eventlisteners** | `eventlisteners/src/main/java/example/eventlisteners/` | Event handling patterns |
| **customscanchecks** | `customscanchecks/src/main/java/example/customscanchecks/` | Scanner API usage (Phase 4) |
| **persistence** | `persistence/src/main/java/example/persistence/` | Settings storage (Phase 1) |
| **ai** | `ai/src/main/java/example/ai/` | Enhanced capabilities, threading patterns |

### Existing AI Pal Extension (Additional Reference)
**Location:** `../AI Pal/`

Useful patterns to reference:
- `build.gradle.kts` - Gradle configuration with Montoya API dependency
- `src/main/java/Extension.java` - Extension initialization pattern
- `src/main/java/util/ThreadManager.java` - Thread-safe executor pattern

---

## Project Structure

```
burpmcp/
├── README.md
├── CLAUDE.md
├── IMPLEMENTATION_PLAN.md          # This file
│
├── burp-extension/                    # Java Burp Extension
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradlew, gradlew.bat
│   └── src/main/java/burpmcp/
│       ├── BurpMcpExtension.java      # Entry point (Phase 1)
│       ├── config/
│       │   └── ExtensionConfig.java   # Configuration (Phase 1)
│       ├── traffic/
│       │   ├── TrafficStore.java      # Request storage (Phase 2)
│       │   ├── StoredRequest.java     # Data class (Phase 2)
│       │   └── TrafficHttpHandler.java # Traffic capture (Phase 2)
│       ├── websocket/
│       │   ├── WebSocketServer.java   # WS server (Phase 3)
│       │   ├── MessageHandler.java    # JSON-RPC router (Phase 3)
│       │   └── ClientSession.java     # Connection state (Phase 3)
│       ├── rpc/
│       │   ├── RpcRequest.java        # Request DTO (Phase 3)
│       │   ├── RpcResponse.java       # Response DTO (Phase 3)
│       │   ├── RpcMethod.java         # Method interface (Phase 3)
│       │   ├── RpcException.java      # Exception class (Phase 3)
│       │   └── methods/               # (Phase 4)
│       │       ├── GetProxyHistory.java
│       │       ├── GetSitemap.java
│       │       ├── SendRequest.java
│       │       ├── TriggerScan.java
│       │       ├── GetScope.java
│       │       └── ModifyScope.java
│       └── util/
│           ├── JsonUtils.java         # Gson helpers (Phase 1)
│           └── RequestSerializer.java # HTTP serialization (Phase 2)
│
├── mcp-server/                        # TypeScript MCP Server
│   ├── package.json                   # (Phase 5)
│   ├── tsconfig.json                  # (Phase 5)
│   └── src/
│       ├── index.ts                   # Entry point (Phase 5)
│       ├── server.ts                  # MCP server setup (Phase 5)
│       ├── burp-client.ts             # WebSocket client (Phase 5)
│       ├── tools/                     # (Phase 6)
│       │   ├── index.ts
│       │   ├── get-proxy-history.ts
│       │   ├── get-sitemap.ts
│       │   ├── send-request.ts
│       │   ├── trigger-scan.ts
│       │   └── get-scope.ts
│       └── types/
│           ├── burp.ts                # Burp data types (Phase 5)
│           └── rpc.ts                 # JSON-RPC types (Phase 5)
│
└── protocol/
    └── PROTOCOL.md                    # JSON-RPC schema (Shared)
```

---

## Shared Protocol Definition

**ALL PHASES MUST ADHERE TO THIS PROTOCOL**

### JSON-RPC Message Format

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": "unique-uuid-string",
  "method": "method_name",
  "params": { /* method-specific parameters */ }
}
```

**Success Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "unique-uuid-string",
  "result": { /* method-specific result */ }
}
```

**Error Response:**
```json
{
  "jsonrpc": "2.0",
  "id": "unique-uuid-string",
  "error": {
    "code": -32600,
    "message": "Error description",
    "data": { /* optional details */ }
  }
}
```

### RPC Methods

| Method | Params | Returns |
|--------|--------|---------|
| `get_proxy_history` | `{domain: string, limit?: number, method?: string, statusCode?: number}` | `{requests: StoredRequest[], total: number}` |
| `get_sitemap` | `{domain: string, includeParams?: boolean}` | `{entries: SitemapEntry[]}` |
| `send_request` | `{url: string, method?: string, headers?: object, body?: string, followRedirects?: boolean}` | `{statusCode: number, headers: object, body: string, time: number}` |
| `trigger_scan` | `{url: string, crawl?: boolean}` | `{scanId: string, status: string}` |
| `get_scope` | `{}` | `{inScope: string[], outScope: string[]}` |
| `modify_scope` | `{action: "add"|"remove", url: string, type: "include"|"exclude"}` | `{success: boolean}` |

### StoredRequest Schema

```typescript
interface StoredRequest {
  id: number;
  timestamp: number;           // Unix ms
  method: string;              // GET, POST, etc.
  url: string;                 // Full URL
  host: string;
  port: number;
  isHttps: boolean;
  requestHeaders: Record<string, string>;
  requestBody: string;         // Base64 if binary
  statusCode: number;
  responseHeaders: Record<string, string>;
  responseBody: string;        // Base64 if binary, truncated at 100KB
  mimeType: string;
  toolSource: string;          // PROXY, REPEATER, etc.
}
```

### Error Codes

| Code | Name | Description |
|------|------|-------------|
| -32700 | Parse error | Invalid JSON |
| -32600 | Invalid request | Missing required fields |
| -32601 | Method not found | Unknown method name |
| -32602 | Invalid params | Invalid/missing parameters |
| -32603 | Internal error | Server-side error |
| -32001 | Pro required | Feature requires Burp Pro |
| -32002 | Not in scope | Domain not in Burp scope |
| -32003 | Timeout | Operation timed out |

---

## Phase Dependencies Graph

```
Phase 1 (Java Foundation)
    │
    ▼
Phase 2 (Traffic Capture) ◄─────────────────┐
    │                                        │
    ▼                                        │
Phase 3 (WebSocket Server)                   │
    │                                        │
    ├──────────────────────────────────────►│
    │                                        │
    ▼                                        │
Phase 4 (RPC Methods) ◄─────── uses ────────┘
    │
    ▼
Phase 5 (MCP Server Foundation) ◄── depends on Phase 3 for testing
    │
    ▼
Phase 6 (MCP Tools) ◄── depends on Phase 4 methods
    │
    ▼
Phase 7 (Integration & Polish)
```

**Parallelization Opportunities:**
- Phase 5 can START after Phase 3 is complete (needs WebSocket to test against)
- Phase 4 and Phase 5 can run in PARALLEL once Phase 3 is done
- Phase 6 requires both Phase 4 and Phase 5

---

## PHASE 1: Java Foundation

**Status:** Not Started
**Dependencies:** None
**Can Parallelize With:** Nothing (must complete first)
**Estimated Files:** 5

### Reference Files to Consult
Before implementing, study these reference files:
- `../Dev/burp-extensions-montoya-api-examples/helloworld/src/main/java/example/helloworld/HelloWorld.java` - Extension entry point pattern
- `../Dev/burp-extensions-montoya-api-examples/persistence/src/main/java/example/persistence/` - Settings storage pattern
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/BurpExtension.java` - BurpExtension interface
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/persistence/Preferences.java` - Preferences API
- `../AI Pal/build.gradle.kts` - Gradle configuration pattern

### Objective
Set up the Burp extension project structure with Gradle, create the main entry point, and establish basic configuration.

### Files to Create

#### 1.1 `burp-extension/build.gradle.kts`
```kotlin
plugins {
    id("java")
}

group = "com.burpmcp"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.portswigger.burp.extensions:montoya-api:2025.10")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.6")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    manifest {
        attributes["Implementation-Title"] = "BurpMCP"
        attributes["Implementation-Version"] = version
    }
}
```

#### 1.2 `burp-extension/settings.gradle.kts`
```kotlin
rootProject.name = "burp-mcp-extension"
```

#### 1.3 `burp-extension/src/main/java/burpmcp/BurpMcpExtension.java`
```java
package burpmcp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burpmcp.config.ExtensionConfig;

public class BurpMcpExtension implements BurpExtension {
    private MontoyaApi api;
    private ExtensionConfig config;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("BurpMCP");

        this.config = new ExtensionConfig(api.persistence().preferences());

        api.logging().logToOutput("BurpMCP extension loaded");
        api.logging().logToOutput("WebSocket port: " + config.getWebSocketPort());

        // Phase 2: Register HTTP handler
        // Phase 3: Start WebSocket server

        api.extension().registerUnloadingHandler(this::cleanup);
    }

    private void cleanup() {
        api.logging().logToOutput("BurpMCP extension unloading...");
        // Phase 3: Stop WebSocket server
    }
}
```

#### 1.4 `burp-extension/src/main/java/burpmcp/config/ExtensionConfig.java`
```java
package burpmcp.config;

import burp.api.montoya.persistence.Preferences;

public class ExtensionConfig {
    private static final String KEY_WS_PORT = "burpmcp.ws.port";
    private static final String KEY_AUTH_TOKEN = "burpmcp.auth.token";
    private static final String KEY_MAX_REQUESTS = "burpmcp.max.requests";
    private static final String KEY_MAX_BODY_SIZE = "burpmcp.max.body.size";

    private final Preferences prefs;

    public ExtensionConfig(Preferences prefs) {
        this.prefs = prefs;
    }

    public int getWebSocketPort() {
        return prefs.getInteger(KEY_WS_PORT).orElse(8198);
    }

    public void setWebSocketPort(int port) {
        prefs.setInteger(KEY_WS_PORT, port);
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN).orElse("");
    }

    public int getMaxRequestsPerDomain() {
        return prefs.getInteger(KEY_MAX_REQUESTS).orElse(100);
    }

    public int getMaxBodySize() {
        return prefs.getInteger(KEY_MAX_BODY_SIZE).orElse(102400); // 100KB
    }
}
```

#### 1.5 `burp-extension/src/main/java/burpmcp/util/JsonUtils.java`
```java
package burpmcp.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtils {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static Gson getGson() {
        return GSON;
    }
}
```

### Verification
```bash
cd burp-extension
./gradlew build
# Load build/libs/burp-mcp-extension-1.0.0.jar in Burp
# Check Output tab shows "BurpMCP extension loaded"
```

### Completion Criteria
- [ ] Gradle build succeeds
- [ ] JAR loads in Burp without errors
- [ ] Extension name appears in Extensions list
- [ ] Log output shows configuration values

---

## PHASE 2: Traffic Capture

**Status:** Not Started
**Dependencies:** Phase 1 complete
**Can Parallelize With:** Nothing yet
**Estimated Files:** 3

### Reference Files to Consult
Before implementing, study these reference files:
- `../Dev/burp-extensions-montoya-api-examples/httphandler/src/main/java/example/httphandler/MyHttpHandler.java` - HttpHandler implementation pattern
- `../Dev/burp-extensions-montoya-api-examples/httphandler/src/main/java/example/httphandler/HttpHandlerExample.java` - Handler registration
- `../Dev/burp-extensions-montoya-api-examples/customlogger/src/main/java/example/customlogger/` - Request storage pattern
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/http/handler/HttpHandler.java` - HttpHandler interface
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/http/handler/HttpRequestToBeSent.java` - Request access
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/http/handler/HttpResponseReceived.java` - Response access
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/http/message/HttpHeader.java` - Header handling

### Objective
Implement HTTP traffic capture and storage with thread-safe data structures.

### Files to Create

#### 2.1 `burp-extension/src/main/java/burpmcp/traffic/StoredRequest.java`
```java
package burpmcp.traffic;

import java.util.Map;
import java.util.HashMap;

public class StoredRequest {
    private final long id;
    private final long timestamp;
    private final String method;
    private final String url;
    private final String host;
    private final int port;
    private final boolean isHttps;
    private final Map<String, String> requestHeaders;
    private final String requestBody;
    private final int statusCode;
    private final Map<String, String> responseHeaders;
    private final String responseBody;
    private final String mimeType;
    private final String toolSource;

    // Builder pattern constructor
    private StoredRequest(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.method = builder.method;
        this.url = builder.url;
        this.host = builder.host;
        this.port = builder.port;
        this.isHttps = builder.isHttps;
        this.requestHeaders = new HashMap<>(builder.requestHeaders);
        this.requestBody = builder.requestBody;
        this.statusCode = builder.statusCode;
        this.responseHeaders = new HashMap<>(builder.responseHeaders);
        this.responseBody = builder.responseBody;
        this.mimeType = builder.mimeType;
        this.toolSource = builder.toolSource;
    }

    // Getters
    public long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isHttps() { return isHttps; }
    public Map<String, String> getRequestHeaders() { return requestHeaders; }
    public String getRequestBody() { return requestBody; }
    public int getStatusCode() { return statusCode; }
    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public String getResponseBody() { return responseBody; }
    public String getMimeType() { return mimeType; }
    public String getToolSource() { return toolSource; }

    public static class Builder {
        private long id;
        private long timestamp = System.currentTimeMillis();
        private String method;
        private String url;
        private String host;
        private int port;
        private boolean isHttps;
        private Map<String, String> requestHeaders = new HashMap<>();
        private String requestBody = "";
        private int statusCode;
        private Map<String, String> responseHeaders = new HashMap<>();
        private String responseBody = "";
        private String mimeType = "";
        private String toolSource = "";

        public Builder id(long id) { this.id = id; return this; }
        public Builder timestamp(long ts) { this.timestamp = ts; return this; }
        public Builder method(String m) { this.method = m; return this; }
        public Builder url(String u) { this.url = u; return this; }
        public Builder host(String h) { this.host = h; return this; }
        public Builder port(int p) { this.port = p; return this; }
        public Builder isHttps(boolean https) { this.isHttps = https; return this; }
        public Builder requestHeaders(Map<String, String> h) { this.requestHeaders = h; return this; }
        public Builder requestBody(String b) { this.requestBody = b; return this; }
        public Builder statusCode(int s) { this.statusCode = s; return this; }
        public Builder responseHeaders(Map<String, String> h) { this.responseHeaders = h; return this; }
        public Builder responseBody(String b) { this.responseBody = b; return this; }
        public Builder mimeType(String m) { this.mimeType = m; return this; }
        public Builder toolSource(String t) { this.toolSource = t; return this; }

        public StoredRequest build() {
            return new StoredRequest(this);
        }
    }
}
```

#### 2.2 `burp-extension/src/main/java/burpmcp/traffic/TrafficStore.java`
```java
package burpmcp.traffic;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TrafficStore {
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<StoredRequest>> requestsByDomain;
    private final AtomicLong idGenerator;
    private final int maxRequestsPerDomain;

    public TrafficStore(int maxRequestsPerDomain) {
        this.requestsByDomain = new ConcurrentHashMap<>();
        this.idGenerator = new AtomicLong(0);
        this.maxRequestsPerDomain = maxRequestsPerDomain;
    }

    public long store(StoredRequest.Builder builder) {
        long id = idGenerator.incrementAndGet();
        StoredRequest request = builder.id(id).build();
        String domain = request.getHost().toLowerCase();

        ConcurrentLinkedDeque<StoredRequest> queue = requestsByDomain
            .computeIfAbsent(domain, k -> new ConcurrentLinkedDeque<>());

        queue.addFirst(request);

        // Evict oldest if over limit
        while (queue.size() > maxRequestsPerDomain) {
            queue.pollLast();
        }

        return id;
    }

    public List<StoredRequest> getByDomain(String domain, int limit) {
        ConcurrentLinkedDeque<StoredRequest> queue = requestsByDomain.get(domain.toLowerCase());
        if (queue == null) {
            return new ArrayList<>();
        }
        return queue.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<StoredRequest> getByDomain(String domain, int limit, String method, Integer statusCode) {
        ConcurrentLinkedDeque<StoredRequest> queue = requestsByDomain.get(domain.toLowerCase());
        if (queue == null) {
            return new ArrayList<>();
        }
        return queue.stream()
            .filter(r -> method == null || r.getMethod().equalsIgnoreCase(method))
            .filter(r -> statusCode == null || r.getStatusCode() == statusCode)
            .limit(limit)
            .collect(Collectors.toList());
    }

    public int getTotalForDomain(String domain) {
        ConcurrentLinkedDeque<StoredRequest> queue = requestsByDomain.get(domain.toLowerCase());
        return queue == null ? 0 : queue.size();
    }

    public void clearDomain(String domain) {
        requestsByDomain.remove(domain.toLowerCase());
    }

    public void clearAll() {
        requestsByDomain.clear();
    }
}
```

#### 2.3 `burp-extension/src/main/java/burpmcp/traffic/TrafficHttpHandler.java`
```java
package burpmcp.traffic;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burpmcp.config.ExtensionConfig;

import java.util.Map;
import java.util.stream.Collectors;

public class TrafficHttpHandler implements HttpHandler {
    private final MontoyaApi api;
    private final TrafficStore store;
    private final ExtensionConfig config;

    public TrafficHttpHandler(MontoyaApi api, TrafficStore store, ExtensionConfig config) {
        this.api = api;
        this.store = store;
        this.config = config;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
        return RequestToBeSentAction.continueWith(request);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
        try {
            HttpRequestToBeSent request = response.initiatingRequest();

            StoredRequest.Builder builder = new StoredRequest.Builder()
                .timestamp(System.currentTimeMillis())
                .method(request.method())
                .url(request.url())
                .host(request.httpService().host())
                .port(request.httpService().port())
                .isHttps(request.httpService().secure())
                .requestHeaders(headersToMap(request.headers()))
                .requestBody(truncateBody(request.bodyToString()))
                .statusCode(response.statusCode())
                .responseHeaders(headersToMap(response.headers()))
                .responseBody(truncateBody(response.bodyToString()))
                .mimeType(response.mimeType().toString())
                .toolSource(response.toolSource().toolType().toString());

            store.store(builder);

        } catch (Exception e) {
            api.logging().logToError("Error storing request: " + e.getMessage());
        }

        return ResponseReceivedAction.continueWith(response);
    }

    private Map<String, String> headersToMap(java.util.List<HttpHeader> headers) {
        return headers.stream()
            .collect(Collectors.toMap(
                HttpHeader::name,
                HttpHeader::value,
                (v1, v2) -> v1 + ", " + v2
            ));
    }

    private String truncateBody(String body) {
        int maxSize = config.getMaxBodySize();
        if (body == null) return "";
        if (body.length() <= maxSize) return body;
        return body.substring(0, maxSize) + "\n[TRUNCATED - " + body.length() + " bytes total]";
    }
}
```

### Update BurpMcpExtension.java
Add to `initialize()`:
```java
// After config initialization
this.trafficStore = new TrafficStore(config.getMaxRequestsPerDomain());
api.http().registerHttpHandler(new TrafficHttpHandler(api, trafficStore, config));
api.logging().logToOutput("Traffic capture enabled");
```

### Verification
1. Load extension in Burp
2. Browse any website through proxy
3. Extension output should NOT show errors
4. (Phase 3 WebSocket needed to query stored data)

### Completion Criteria
- [ ] TrafficStore stores requests without ConcurrentModificationException
- [ ] Requests are properly keyed by domain
- [ ] Oldest requests evicted when limit reached
- [ ] Body truncation works correctly

---

## PHASE 3: WebSocket Server

**Status:** Not Started
**Dependencies:** Phase 2 complete
**Can Parallelize With:** Phase 5 can START once this completes
**Estimated Files:** 5

### Reference Files to Consult
Before implementing, study these reference files:
- `../Dev/burp-extensions-montoya-api-examples/ai/src/main/java/example/ai/AiExample.java` - Threading pattern with ExecutorService
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/logging/Logging.java` - Logging interface
- `../AI Pal/src/main/java/util/ThreadManager.java` - Thread management pattern
- Java-WebSocket library docs: https://github.com/TooTallNate/Java-WebSocket

**Note:** This phase uses external Java-WebSocket library (org.java-websocket:Java-WebSocket:1.5.6), not Montoya API directly.

### Objective
Implement WebSocket server for JSON-RPC communication.

### Files to Create

#### 3.1 `burp-extension/src/main/java/burpmcp/rpc/RpcRequest.java`
```java
package burpmcp.rpc;

import com.google.gson.JsonObject;

public class RpcRequest {
    private String jsonrpc;
    private String id;
    private String method;
    private JsonObject params;

    public boolean isValid() {
        return "2.0".equals(jsonrpc) && id != null && method != null;
    }

    public String getJsonrpc() { return jsonrpc; }
    public String getId() { return id; }
    public String getMethod() { return method; }
    public JsonObject getParams() { return params != null ? params : new JsonObject(); }
}
```

#### 3.2 `burp-extension/src/main/java/burpmcp/rpc/RpcResponse.java`
```java
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
```

#### 3.3 `burp-extension/src/main/java/burpmcp/rpc/RpcMethod.java`
```java
package burpmcp.rpc;

import com.google.gson.JsonObject;

public interface RpcMethod {
    String getName();
    Object execute(JsonObject params) throws RpcException;
}
```

#### 3.4 `burp-extension/src/main/java/burpmcp/rpc/RpcException.java`
```java
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
```

#### 3.5 `burp-extension/src/main/java/burpmcp/websocket/MessageHandler.java`
```java
package burpmcp.websocket;

import burpmcp.rpc.*;
import burpmcp.util.JsonUtils;
import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.HashMap;

public class MessageHandler {
    private final Map<String, RpcMethod> methods = new HashMap<>();

    public void registerMethod(RpcMethod method) {
        methods.put(method.getName(), method);
    }

    public String handleMessage(String message) {
        RpcRequest request;
        try {
            request = JsonUtils.fromJson(message, RpcRequest.class);
        } catch (JsonSyntaxException e) {
            return JsonUtils.toJson(RpcResponse.error(null, RpcException.PARSE_ERROR, "Parse error"));
        }

        if (!request.isValid()) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.INVALID_REQUEST, "Invalid request"));
        }

        RpcMethod method = methods.get(request.getMethod());
        if (method == null) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.METHOD_NOT_FOUND,
                "Method not found: " + request.getMethod()));
        }

        try {
            Object result = method.execute(request.getParams());
            return JsonUtils.toJson(RpcResponse.success(request.getId(), result));
        } catch (RpcException e) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return JsonUtils.toJson(RpcResponse.error(request.getId(), RpcException.INTERNAL_ERROR, e.getMessage()));
        }
    }
}
```

#### 3.6 `burp-extension/src/main/java/burpmcp/websocket/WebSocketServer.java`
```java
package burpmcp.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer as JWebSocketServer;
import burp.api.montoya.logging.Logging;

import java.net.InetSocketAddress;

public class WebSocketServer extends JWebSocketServer {
    private final Logging logging;
    private final MessageHandler messageHandler;
    private final String authToken;

    public WebSocketServer(int port, Logging logging, MessageHandler handler, String authToken) {
        super(new InetSocketAddress("127.0.0.1", port));
        this.logging = logging;
        this.messageHandler = handler;
        this.authToken = authToken;
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!authToken.isEmpty()) {
            String clientToken = handshake.getFieldValue("Authorization");
            if (!("Bearer " + authToken).equals(clientToken)) {
                conn.close(4001, "Unauthorized");
                logging.logToOutput("WebSocket connection rejected: Invalid token");
                return;
            }
        }
        logging.logToOutput("WebSocket client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logging.logToOutput("WebSocket client disconnected: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String response = messageHandler.handleMessage(message);
        conn.send(response);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logging.logToError("WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        logging.logToOutput("WebSocket server started on port " + getPort());
    }

    public void shutdown() {
        try {
            stop(1000);
            logging.logToOutput("WebSocket server stopped");
        } catch (InterruptedException e) {
            logging.logToError("Error stopping WebSocket server: " + e.getMessage());
        }
    }
}
```

### Update BurpMcpExtension.java
```java
// Add fields
private WebSocketServer wsServer;
private MessageHandler messageHandler;

// In initialize()
this.messageHandler = new MessageHandler();
// Phase 4: Register RPC methods here

this.wsServer = new WebSocketServer(
    config.getWebSocketPort(),
    api.logging(),
    messageHandler,
    config.getAuthToken()
);
wsServer.start();

// In cleanup()
if (wsServer != null) {
    wsServer.shutdown();
}
```

### Verification
```bash
# Install wscat for testing
npm install -g wscat

# Connect to WebSocket server
wscat -c ws://localhost:8198

# Send test message (should return method not found)
{"jsonrpc":"2.0","id":"1","method":"test","params":{}}
```

### Completion Criteria
- [ ] WebSocket server starts on configured port
- [ ] Clients can connect
- [ ] JSON-RPC messages are parsed correctly
- [ ] Error responses follow JSON-RPC 2.0 spec
- [ ] Auth token validation works (when configured)

---

## PHASE 4: RPC Methods

**Status:** Not Started
**Dependencies:** Phase 3 complete
**Can Parallelize With:** Phase 5, Phase 6 (once Phase 5 complete)
**Estimated Files:** 6

### Reference Files to Consult
Before implementing, study these reference files:

**For GetProxyHistory (uses TrafficStore from Phase 2):**
- No additional references needed, uses Phase 2 TrafficStore

**For GetSitemap:**
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/sitemap/SiteMap.java` - SiteMap interface
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/sitemap/SiteMapFilter.java` - Filtering sitemap

**For SendRequest:**
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/http/Http.java` - sendRequest method
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/http/message/requests/HttpRequest.java` - Request building (httpRequestFromUrl, withMethod, withHeader, withBody)
- `../Dev/burp-extensions-montoya-api-examples/httphandler/src/main/java/example/httphandler/MyHttpHandler.java` - Request modification pattern

**For TriggerScan (Burp Pro only):**
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/scanner/Scanner.java` - Scanner interface
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/scanner/AuditConfiguration.java` - Audit configuration
- `../Dev/burp-extensions-montoya-api-examples/customscanchecks/src/main/java/example/customscanchecks/` - Scanner usage pattern

**For GetScope/ModifyScope:**
- `../Dev/burp-extensions-montoya-api/src/main/java/burp/api/montoya/scope/Scope.java` - Scope interface (includeInScope, excludeFromScope, isInScope)

### Objective
Implement all RPC methods for Burp Suite operations.

### Files to Create

#### 4.1 `burp-extension/src/main/java/burpmcp/rpc/methods/GetProxyHistory.java`
```java
package burpmcp.rpc.methods;

import burpmcp.rpc.*;
import burpmcp.traffic.TrafficStore;
import burpmcp.traffic.StoredRequest;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class GetProxyHistory implements RpcMethod {
    private final TrafficStore store;

    public GetProxyHistory(TrafficStore store) {
        this.store = store;
    }

    @Override
    public String getName() {
        return "get_proxy_history";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String domain = params.has("domain") ? params.get("domain").getAsString() : null;
        if (domain == null || domain.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "domain parameter required");
        }

        int limit = params.has("limit") ? params.get("limit").getAsInt() : 50;
        String method = params.has("method") ? params.get("method").getAsString() : null;
        Integer statusCode = params.has("statusCode") ? params.get("statusCode").getAsInt() : null;

        List<StoredRequest> requests = store.getByDomain(domain, limit, method, statusCode);
        int total = store.getTotalForDomain(domain);

        Map<String, Object> result = new HashMap<>();
        result.put("requests", requests);
        result.put("total", total);
        result.put("returned", requests.size());

        return result;
    }
}
```

#### 4.2 `burp-extension/src/main/java/burpmcp/rpc/methods/GetSitemap.java`
```java
package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.sitemap.SiteMapFilter;
import burp.api.montoya.http.message.HttpRequestResponse;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class GetSitemap implements RpcMethod {
    private final MontoyaApi api;

    public GetSitemap(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_sitemap";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String domain = params.has("domain") ? params.get("domain").getAsString() : null;
        boolean includeParams = !params.has("includeParams") || params.get("includeParams").getAsBoolean();

        List<HttpRequestResponse> items;
        if (domain != null && !domain.isEmpty()) {
            items = new ArrayList<>(api.siteMap().requestResponses(
                SiteMapFilter.prefixFilter("https://" + domain)
            ));
            items.addAll(api.siteMap().requestResponses(
                SiteMapFilter.prefixFilter("http://" + domain)
            ));
        } else {
            items = api.siteMap().requestResponses();
        }

        List<Map<String, Object>> entries = items.stream()
            .map(item -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("url", item.request().url());
                entry.put("method", item.request().method());
                entry.put("statusCode", item.response() != null ? item.response().statusCode() : 0);
                entry.put("mimeType", item.response() != null ? item.response().mimeType().toString() : "");
                if (includeParams) {
                    entry.put("parameters", item.request().parameters().stream()
                        .map(p -> p.name())
                        .collect(Collectors.toList()));
                }
                return entry;
            })
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("entries", entries);
        result.put("count", entries.size());

        return result;
    }
}
```

#### 4.3 `burp-extension/src/main/java/burpmcp/rpc/methods/SendRequest.java`
```java
package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burpmcp.rpc.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class SendRequest implements RpcMethod {
    private final MontoyaApi api;

    public SendRequest(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "send_request";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String url = params.has("url") ? params.get("url").getAsString() : null;
        if (url == null || url.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "url parameter required");
        }

        String method = params.has("method") ? params.get("method").getAsString() : "GET";
        String body = params.has("body") ? params.get("body").getAsString() : "";

        HttpRequest request = HttpRequest.httpRequestFromUrl(url).withMethod(method);

        if (params.has("headers")) {
            JsonObject headers = params.getAsJsonObject("headers");
            for (Map.Entry<String, JsonElement> entry : headers.entrySet()) {
                request = request.withHeader(entry.getKey(), entry.getValue().getAsString());
            }
        }

        if (!body.isEmpty()) {
            request = request.withBody(body);
        }

        long startTime = System.currentTimeMillis();
        HttpRequestResponse response = api.http().sendRequest(request);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.response().statusCode());
        result.put("headers", headersToMap(response.response().headers()));
        result.put("body", response.response().bodyToString());
        result.put("time", duration);

        return result;
    }

    private Map<String, String> headersToMap(java.util.List<burp.api.montoya.http.message.HttpHeader> headers) {
        Map<String, String> map = new HashMap<>();
        for (var h : headers) {
            map.put(h.name(), h.value());
        }
        return map;
    }
}
```

#### 4.4 `burp-extension/src/main/java/burpmcp/rpc/methods/TriggerScan.java`
```java
package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TriggerScan implements RpcMethod {
    private final MontoyaApi api;

    public TriggerScan(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "trigger_scan";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        try {
            api.scanner();
        } catch (UnsupportedOperationException e) {
            throw new RpcException(RpcException.PRO_REQUIRED, "Active scanning requires Burp Suite Professional");
        }

        String url = params.has("url") ? params.get("url").getAsString() : null;
        if (url == null || url.isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "url parameter required");
        }

        HttpRequest request = HttpRequest.httpRequestFromUrl(url);

        api.scanner().startAudit(
            burp.api.montoya.scanner.AuditConfiguration.auditConfiguration(
                BuiltInAuditConfiguration.LIGHT_ACTIVE
            )
        ).addRequestResponse(
            burp.api.montoya.http.message.HttpRequestResponse.httpRequestResponse(request, null)
        );

        String scanId = UUID.randomUUID().toString();

        Map<String, Object> result = new HashMap<>();
        result.put("scanId", scanId);
        result.put("status", "queued");
        result.put("message", "Scan started for: " + url);

        return result;
    }
}
```

#### 4.5 `burp-extension/src/main/java/burpmcp/rpc/methods/GetScope.java`
```java
package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class GetScope implements RpcMethod {
    private final MontoyaApi api;

    public GetScope(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_scope";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Use is_in_scope to check if a URL is in scope");
        result.put("checkEndpoint", "Call with {url: 'http://example.com'} to check");
        return result;
    }
}
```

#### 4.6 `burp-extension/src/main/java/burpmcp/rpc/methods/ModifyScope.java`
```java
package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ModifyScope implements RpcMethod {
    private final MontoyaApi api;

    public ModifyScope(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "modify_scope";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String action = params.has("action") ? params.get("action").getAsString() : null;
        String url = params.has("url") ? params.get("url").getAsString() : null;

        if (action == null || url == null) {
            throw new RpcException(RpcException.INVALID_PARAMS, "action and url parameters required");
        }

        if ("add".equalsIgnoreCase(action)) {
            api.scope().includeInScope(url);
        } else if ("remove".equalsIgnoreCase(action)) {
            api.scope().excludeFromScope(url);
        } else {
            throw new RpcException(RpcException.INVALID_PARAMS, "action must be 'add' or 'remove'");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("action", action);
        result.put("url", url);

        return result;
    }
}
```

### Register Methods in BurpMcpExtension.java
```java
// After creating messageHandler
messageHandler.registerMethod(new GetProxyHistory(trafficStore));
messageHandler.registerMethod(new GetSitemap(api));
messageHandler.registerMethod(new SendRequest(api));
messageHandler.registerMethod(new TriggerScan(api));
messageHandler.registerMethod(new GetScope(api));
messageHandler.registerMethod(new ModifyScope(api));
```

### Verification
```bash
wscat -c ws://localhost:8198

# Test get_proxy_history
{"jsonrpc":"2.0","id":"1","method":"get_proxy_history","params":{"domain":"example.com","limit":5}}

# Test get_sitemap
{"jsonrpc":"2.0","id":"2","method":"get_sitemap","params":{"domain":"example.com"}}

# Test send_request
{"jsonrpc":"2.0","id":"3","method":"send_request","params":{"url":"https://httpbin.org/get"}}
```

### Completion Criteria
- [ ] All 6 RPC methods implemented
- [ ] Each method validates parameters
- [ ] Error responses include appropriate codes
- [ ] Pro-only features detected and handled

---

## PHASE 5: MCP Server Foundation

**Status:** Not Started
**Dependencies:** Phase 3 complete (needs WebSocket server to test)
**Can Parallelize With:** Phase 4
**Estimated Files:** 7

### Reference Files to Consult
This phase is TypeScript/Node.js, not Java. Key references:
- MCP SDK: https://github.com/modelcontextprotocol/typescript-sdk
- MCP Server docs: https://modelcontextprotocol.io/docs/server
- WebSocket client (ws): https://github.com/websockets/ws

**Note:** This phase does NOT use Montoya API - it's the TypeScript side that connects TO the Burp extension.

### Objective
Set up TypeScript MCP server with WebSocket client to Burp extension.

### Files to Create

#### 5.1 `mcp-server/package.json`
```json
{
  "name": "burp-mcp-server",
  "version": "1.0.0",
  "description": "MCP server for Burp Suite integration",
  "type": "module",
  "main": "dist/index.js",
  "bin": {
    "burp-mcp": "./dist/index.js"
  },
  "scripts": {
    "build": "tsc",
    "start": "node dist/index.js",
    "dev": "tsx src/index.ts"
  },
  "dependencies": {
    "@modelcontextprotocol/sdk": "^1.0.0",
    "ws": "^8.16.0",
    "zod": "^3.23.0"
  },
  "devDependencies": {
    "@types/node": "^20.11.0",
    "@types/ws": "^8.5.10",
    "typescript": "^5.3.0",
    "tsx": "^4.7.0"
  },
  "engines": {
    "node": ">=18.0.0"
  }
}
```

#### 5.2 `mcp-server/tsconfig.json`
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "declaration": true,
    "sourceMap": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

#### 5.3 `mcp-server/src/types/rpc.ts`
```typescript
export interface RpcRequest {
  jsonrpc: '2.0';
  id: string;
  method: string;
  params?: Record<string, unknown>;
}

export interface RpcResponse<T = unknown> {
  jsonrpc: '2.0';
  id: string;
  result?: T;
  error?: RpcError;
}

export interface RpcError {
  code: number;
  message: string;
  data?: unknown;
}
```

#### 5.4 `mcp-server/src/types/burp.ts`
```typescript
export interface StoredRequest {
  id: number;
  timestamp: number;
  method: string;
  url: string;
  host: string;
  port: number;
  isHttps: boolean;
  requestHeaders: Record<string, string>;
  requestBody: string;
  statusCode: number;
  responseHeaders: Record<string, string>;
  responseBody: string;
  mimeType: string;
  toolSource: string;
}

export interface SitemapEntry {
  url: string;
  method: string;
  statusCode: number;
  mimeType: string;
  parameters?: string[];
}

export interface ProxyHistoryResult {
  requests: StoredRequest[];
  total: number;
  returned: number;
}

export interface SitemapResult {
  entries: SitemapEntry[];
  count: number;
}

export interface SendRequestResult {
  statusCode: number;
  headers: Record<string, string>;
  body: string;
  time: number;
}

export interface ScanResult {
  scanId: string;
  status: string;
  message: string;
}
```

#### 5.5 `mcp-server/src/burp-client.ts`
```typescript
import WebSocket from 'ws';
import { RpcRequest, RpcResponse } from './types/rpc.js';

interface PendingRequest {
  resolve: (value: unknown) => void;
  reject: (error: Error) => void;
  timeout: NodeJS.Timeout;
}

export class BurpClient {
  private ws: WebSocket | null = null;
  private url: string;
  private authToken: string;
  private pendingRequests: Map<string, PendingRequest> = new Map();
  private requestId = 0;
  private reconnectDelay: number;
  private connected = false;

  constructor(options: { url: string; authToken?: string; reconnectDelay?: number }) {
    this.url = options.url;
    this.authToken = options.authToken || '';
    this.reconnectDelay = options.reconnectDelay || 5000;
  }

  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      const headers: Record<string, string> = {};
      if (this.authToken) {
        headers['Authorization'] = `Bearer ${this.authToken}`;
      }

      this.ws = new WebSocket(this.url, { headers });

      this.ws.on('open', () => {
        this.connected = true;
        console.error('[BurpClient] Connected to Burp extension');
        resolve();
      });

      this.ws.on('message', (data: WebSocket.Data) => {
        this.handleMessage(data.toString());
      });

      this.ws.on('close', () => {
        this.connected = false;
        console.error('[BurpClient] Disconnected from Burp extension');
        this.scheduleReconnect();
      });

      this.ws.on('error', (error) => {
        console.error('[BurpClient] WebSocket error:', error.message);
        if (!this.connected) {
          reject(error);
        }
      });
    });
  }

  private scheduleReconnect(): void {
    setTimeout(() => {
      console.error('[BurpClient] Attempting to reconnect...');
      this.connect().catch(() => {});
    }, this.reconnectDelay);
  }

  private handleMessage(data: string): void {
    try {
      const response: RpcResponse = JSON.parse(data);
      const pending = this.pendingRequests.get(response.id);

      if (pending) {
        clearTimeout(pending.timeout);
        this.pendingRequests.delete(response.id);

        if (response.error) {
          pending.reject(new Error(`${response.error.code}: ${response.error.message}`));
        } else {
          pending.resolve(response.result);
        }
      }
    } catch (error) {
      console.error('[BurpClient] Failed to parse response:', error);
    }
  }

  async call<T>(method: string, params: Record<string, unknown> = {}): Promise<T> {
    if (!this.connected || !this.ws) {
      throw new Error('Not connected to Burp extension');
    }

    const id = String(++this.requestId);
    const request: RpcRequest = {
      jsonrpc: '2.0',
      id,
      method,
      params
    };

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pendingRequests.delete(id);
        reject(new Error('Request timed out'));
      }, 30000);

      this.pendingRequests.set(id, { resolve: resolve as (v: unknown) => void, reject, timeout });
      this.ws!.send(JSON.stringify(request));
    });
  }

  isConnected(): boolean {
    return this.connected;
  }

  close(): void {
    if (this.ws) {
      this.ws.close();
    }
  }
}
```

#### 5.6 `mcp-server/src/server.ts`
```typescript
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { BurpClient } from './burp-client.js';

export async function createServer(burpClient: BurpClient): Promise<McpServer> {
  const server = new McpServer({
    name: 'burp-mcp',
    version: '1.0.0'
  });

  // Phase 6: Register tools here

  return server;
}

export async function startServer(server: McpServer): Promise<void> {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}
```

#### 5.7 `mcp-server/src/index.ts`
```typescript
#!/usr/bin/env node

import { BurpClient } from './burp-client.js';
import { createServer, startServer } from './server.js';

async function main(): Promise<void> {
  const burpUrl = process.env.BURP_WS_URL || 'ws://localhost:8198';
  const authToken = process.env.BURP_AUTH_TOKEN || '';

  console.error(`[BurpMCP] Starting MCP server...`);
  console.error(`[BurpMCP] Connecting to Burp at: ${burpUrl}`);

  const burpClient = new BurpClient({
    url: burpUrl,
    authToken,
    reconnectDelay: parseInt(process.env.BURP_RECONNECT_DELAY || '5000', 10)
  });

  try {
    await burpClient.connect();
  } catch (error) {
    console.error(`[BurpMCP] Failed to connect to Burp extension. Make sure it's running.`);
    console.error(`[BurpMCP] Will retry connection in background...`);
  }

  const server = await createServer(burpClient);
  await startServer(server);

  console.error('[BurpMCP] MCP server running');
}

main().catch((error) => {
  console.error('[BurpMCP] Fatal error:', error);
  process.exit(1);
});
```

### Verification
```bash
cd mcp-server
npm install
npm run build

# Test standalone (with Burp extension running)
node dist/index.js
# Should see: [BurpMCP] Connected to Burp extension
```

### Completion Criteria
- [ ] TypeScript compiles without errors
- [ ] BurpClient connects to Burp WebSocket server
- [ ] Reconnection logic works when Burp restarts
- [ ] MCP server initializes with stdio transport

---

## PHASE 6: MCP Tools

**Status:** Not Started
**Dependencies:** Phase 4 and Phase 5 complete
**Can Parallelize With:** Nothing (final phase before integration)
**Estimated Files:** 6

### Reference Files to Consult
This phase is TypeScript/Node.js. Key references:
- MCP Tool definition: https://modelcontextprotocol.io/docs/server/tools
- Zod schema validation: https://zod.dev/
- Phase 4 RPC methods (must match parameter/return schemas)

**Important:** The MCP tool parameters and return types MUST match the JSON-RPC schemas defined in Phase 4. Cross-reference the Shared Protocol Definition section.

### Objective
Implement MCP tool handlers that call Burp extension RPC methods.

### Files to Create

#### 6.1 `mcp-server/src/tools/get-proxy-history.ts`
```typescript
import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { ProxyHistoryResult } from '../types/burp.js';

export function registerGetProxyHistory(server: McpServer, client: BurpClient): void {
  server.tool(
    'get_proxy_history',
    'Get HTTP requests/responses captured by Burp proxy for a specific domain',
    {
      domain: z.string().describe('Target domain (e.g., "example.com")'),
      limit: z.number().optional().default(50).describe('Maximum requests to return (default: 50)'),
      method: z.string().optional().describe('Filter by HTTP method (e.g., "POST")'),
      statusCode: z.number().optional().describe('Filter by response status code')
    },
    async (params) => {
      const result = await client.call<ProxyHistoryResult>('get_proxy_history', params);
      return {
        content: [{ type: 'text', text: JSON.stringify(result, null, 2) }]
      };
    }
  );
}
```

#### 6.2 `mcp-server/src/tools/get-sitemap.ts`
```typescript
import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { SitemapResult } from '../types/burp.js';

export function registerGetSitemap(server: McpServer, client: BurpClient): void {
  server.tool(
    'get_sitemap',
    'Get discovered endpoints from Burp\'s site map for a domain',
    {
      domain: z.string().optional().describe('Target domain (optional, returns all if not specified)'),
      includeParams: z.boolean().optional().default(true).describe('Include parameter names')
    },
    async (params) => {
      const result = await client.call<SitemapResult>('get_sitemap', params);
      return {
        content: [{ type: 'text', text: JSON.stringify(result, null, 2) }]
      };
    }
  );
}
```

#### 6.3 `mcp-server/src/tools/send-request.ts`
```typescript
import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { SendRequestResult } from '../types/burp.js';

export function registerSendRequest(server: McpServer, client: BurpClient): void {
  server.tool(
    'send_request',
    'Send a custom HTTP request through Burp Suite',
    {
      url: z.string().url().describe('Target URL'),
      method: z.string().optional().default('GET').describe('HTTP method'),
      headers: z.record(z.string()).optional().describe('Request headers'),
      body: z.string().optional().describe('Request body'),
      followRedirects: z.boolean().optional().default(false).describe('Follow redirects')
    },
    async (params) => {
      const result = await client.call<SendRequestResult>('send_request', params);
      return {
        content: [{
          type: 'text',
          text: `Status: ${result.statusCode}\nTime: ${result.time}ms\n\nHeaders:\n${JSON.stringify(result.headers, null, 2)}\n\nBody:\n${result.body}`
        }]
      };
    }
  );
}
```

#### 6.4 `mcp-server/src/tools/trigger-scan.ts`
```typescript
import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { ScanResult } from '../types/burp.js';

export function registerTriggerScan(server: McpServer, client: BurpClient): void {
  server.tool(
    'trigger_scan',
    'Start an active vulnerability scan on a URL (Burp Professional only)',
    {
      url: z.string().url().describe('URL to scan'),
      crawl: z.boolean().optional().default(false).describe('Crawl before scanning')
    },
    async (params) => {
      const result = await client.call<ScanResult>('trigger_scan', params);
      return {
        content: [{
          type: 'text',
          text: `Scan ${result.status}: ${result.message}\nScan ID: ${result.scanId}`
        }]
      };
    }
  );
}
```

#### 6.5 `mcp-server/src/tools/get-scope.ts`
```typescript
import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';

export function registerGetScope(server: McpServer, client: BurpClient): void {
  server.tool(
    'get_scope',
    'Get the current Burp Suite scope configuration',
    {},
    async () => {
      const result = await client.call<Record<string, unknown>>('get_scope', {});
      return {
        content: [{ type: 'text', text: JSON.stringify(result, null, 2) }]
      };
    }
  );
}

export function registerModifyScope(server: McpServer, client: BurpClient): void {
  server.tool(
    'modify_scope',
    'Add or remove a URL from Burp\'s scope',
    {
      action: z.enum(['add', 'remove']).describe('Action to perform'),
      url: z.string().describe('URL to add/remove from scope')
    },
    async (params) => {
      const result = await client.call<Record<string, unknown>>('modify_scope', params);
      return {
        content: [{ type: 'text', text: JSON.stringify(result, null, 2) }]
      };
    }
  );
}
```

#### 6.6 `mcp-server/src/tools/index.ts`
```typescript
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { registerGetProxyHistory } from './get-proxy-history.js';
import { registerGetSitemap } from './get-sitemap.js';
import { registerSendRequest } from './send-request.js';
import { registerTriggerScan } from './trigger-scan.js';
import { registerGetScope, registerModifyScope } from './get-scope.js';

export function registerAllTools(server: McpServer, client: BurpClient): void {
  registerGetProxyHistory(server, client);
  registerGetSitemap(server, client);
  registerSendRequest(server, client);
  registerTriggerScan(server, client);
  registerGetScope(server, client);
  registerModifyScope(server, client);
}
```

### Update server.ts
```typescript
import { registerAllTools } from './tools/index.js';

export async function createServer(burpClient: BurpClient): Promise<McpServer> {
  const server = new McpServer({
    name: 'burp-mcp',
    version: '1.0.0'
  });

  registerAllTools(server, burpClient);

  return server;
}
```

### Verification
```bash
cd mcp-server
npm run build
```

### Completion Criteria
- [ ] All 6 tools registered
- [ ] Input validation via Zod schemas
- [ ] Proper error handling
- [ ] Response formatting for Claude

---

## PHASE 7: Integration & Polish

**Status:** Not Started
**Dependencies:** All previous phases complete
**Can Parallelize With:** N/A (final phase)

### Objective
Full integration testing, documentation, and polish.

### Tasks

#### 7.1 Claude Code Configuration
Create or update `~/.claude.json`:
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

#### 7.2 Create README.md
Document:
- Installation steps
- Configuration options
- Usage examples
- Troubleshooting

#### 7.3 Create CLAUDE.md
```markdown
# BurpMCP - Claude Code Integration

## Available Tools

- `get_proxy_history` - Query HTTP traffic by domain
- `get_sitemap` - Get discovered endpoints
- `send_request` - Send custom requests
- `trigger_scan` - Start vulnerability scans
- `get_scope` / `modify_scope` - Manage scope

## Example Prompts

- "Get the last 20 requests for api.example.com and analyze authentication"
- "Send a POST request to /api/login with test credentials"
- "Show me the sitemap for target.com"
```

### Final Verification Checklist

1. **Build Extension:**
   ```bash
   cd burp-extension && ./gradlew build
   ```

2. **Load in Burp:**
   - Extensions > Add > Select JAR
   - Verify "BurpMCP" appears
   - Check Output for startup messages

3. **Test WebSocket:**
   ```bash
   wscat -c ws://localhost:8198
   {"jsonrpc":"2.0","id":"1","method":"get_proxy_history","params":{"domain":"test.com"}}
   ```

4. **Build MCP Server:**
   ```bash
   cd mcp-server && npm install && npm run build
   ```

5. **Configure Claude Code:**
   - Add to `~/.claude.json`
   - Restart Claude Code

6. **Verify Tools:**
   ```
   /mcp
   # Should list: get_proxy_history, get_sitemap, send_request, etc.
   ```

7. **End-to-End Test:**
   - Browse target site through Burp proxy
   - In Claude Code: "Analyze the last 10 requests to target.com"
   - Verify response includes captured traffic

### Completion Criteria
- [ ] All tools visible in `/mcp`
- [ ] Claude can call tools successfully
- [ ] Errors handled gracefully
- [ ] Documentation complete
