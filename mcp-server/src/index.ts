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
