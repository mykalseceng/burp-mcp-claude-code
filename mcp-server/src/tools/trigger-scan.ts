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
