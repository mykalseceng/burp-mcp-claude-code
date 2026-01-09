import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { StopScanResult } from '../types/burp.js';

export function registerStopScan(server: McpServer, client: BurpClient): void {
  server.tool(
    'stop_scan',
    'Stop a running scan by its ID (returned from trigger_scan)',
    {
      scanId: z.string().describe('The scan ID returned from trigger_scan')
    },
    async (params) => {
      const result = await client.call<StopScanResult>('stop_scan', params);

      return {
        content: [{
          type: 'text',
          text: `Scan stopped: ${result.message}\nCrawl stopped: ${result.crawlStopped}\nAudit stopped: ${result.auditStopped}`
        }]
      };
    }
  );
}
