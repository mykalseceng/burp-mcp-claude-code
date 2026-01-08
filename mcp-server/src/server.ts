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
