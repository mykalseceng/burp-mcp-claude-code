import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';

export function registerSendToRepeater(server: McpServer, client: BurpClient): void {
  server.tool(
    'send_to_repeater',
    'Send an HTTP request to Burp Repeater for manual testing. Claude can identify interesting requests and set them up for hands-on exploitation.',
    {
      request: z.string().describe('Raw HTTP request content (headers and body)'),
      host: z.string().describe('Target hostname'),
      port: z.number().optional().default(443).describe('Target port (default: 443)'),
      https: z.boolean().optional().default(true).describe('Use HTTPS (default: true)'),
      tabName: z.string().optional().describe('Name for the Repeater tab')
    },
    async (params) => {
      const result = await client.call<{ success: boolean; message: string }>('send_to_repeater', params);

      return {
        content: [{
          type: 'text',
          text: result.message
        }]
      };
    }
  );
}
