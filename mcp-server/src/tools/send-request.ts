import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { SendRequestResult } from '../types/burp.js';

export function registerSendRequest(server: McpServer, client: BurpClient): void {
  server.tool(
    'send_request',
    'Send a custom HTTP request through Burp Suite',
    {
      url: z.string().url().describe('Target URL'),
      method: z.string().optional().default('GET').describe('HTTP method'),
      headers: z.record(z.string()).optional().describe('Request headers'),
      body: z.string().optional().describe('Request body'),
      followRedirects: z.boolean().optional().default(false).describe('Follow redirects')
    },
    async (params) => {
      const result = await client.call<SendRequestResult>('send_request', params);

      return {
        content: [{
          type: 'text',
          text: `Status: ${result.statusCode}\nTime: ${result.time}ms\n\nHeaders:\n${JSON.stringify(result.headers, null, 2)}\n\nBody:\n${result.body}`
        }]
      };
    }
  );
}
