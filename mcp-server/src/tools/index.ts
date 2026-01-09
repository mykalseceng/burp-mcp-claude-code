import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { registerGetProxyHistory } from './get-proxy-history.js';
import { registerGetSitemap } from './get-sitemap.js';
import { registerSendRequest } from './send-request.js';
import { registerStartScan } from './start-scan.js';
import { registerGetScope, registerModifyScope } from './get-scope.js';

export function registerAllTools(server: McpServer, client: BurpClient): void {
  registerGetProxyHistory(server, client);
  registerGetSitemap(server, client);
  registerSendRequest(server, client);
  registerStartScan(server, client);
  registerGetScope(server, client);
  registerModifyScope(server, client);
}
