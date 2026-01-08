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
        content: [{
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }]
      };
    }
  );
}
