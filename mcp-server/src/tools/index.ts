import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { registerGetProxyHistory } from './get-proxy-history.js';
import { registerGetSitemap } from './get-sitemap.js';
import { registerSendRequest } from './send-request.js';
import { registerStartScan } from './start-scan.js';
import { registerStopScan } from './stop-scan.js';
import { registerGetScope, registerModifyScope } from './get-scope.js';
import { registerGetScannerIssues } from './get-scanner-issues.js';
import { registerSendToRepeater } from './send-to-repeater.js';
import { registerSendToIntruder } from './send-to-intruder.js';

export function registerAllTools(server: McpServer, client: BurpClient): void {
  registerGetProxyHistory(server, client);
  registerGetSitemap(server, client);
  registerSendRequest(server, client);
  registerStartScan(server, client);
  registerStopScan(server, client);
  registerGetScope(server, client);
  registerModifyScope(server, client);
  registerGetScannerIssues(server, client);
  registerSendToRepeater(server, client);
  registerSendToIntruder(server, client);
}
