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
        content: [{
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }]
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
        content: [{
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }]
      };
    }
  );
}
