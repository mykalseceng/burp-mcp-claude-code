import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { SendToIntruderResult } from '../types/burp.js';

export function registerSendToIntruder(server: McpServer, client: BurpClient): void {
  server.tool(
    'send_to_intruder',
    'Send an HTTP request to Burp Intruder for fuzzing/brute-force attacks. Claude can identify injection points and set up targeted attacks.',
    {
      request: z.string().describe('Raw HTTP request content (headers and body)'),
      host: z.string().describe('Target hostname'),
      port: z.number().optional().default(443).describe('Target port (default: 443)'),
      https: z.boolean().optional().default(true).describe('Use HTTPS (default: true)'),
      tabName: z.string().optional().describe('Name for the Intruder tab')
    },
    async (params) => {
      const result = await client.call<SendToIntruderResult>('send_to_intruder', params);
      return {
        content: [{
          type: 'text',
          text: result.message
        }]
      };
    }
  );
}
