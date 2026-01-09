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
      followRedirects: z.boolean().optional().default(false).describe('Follow redirects'),
      addToSiteMap: z.boolean().optional().default(false).describe('Add request/response to Burp Site Map'),
      source: z.string().optional().default('Claude Code').describe('Source identifier for the request (shown in Site Map annotations)')
    },
    async (params) => {
      const result = await client.call<SendRequestResult>('send_request', params);

      const siteMapInfo = result.addedToSiteMap ? `\nAdded to Site Map: Yes (Source: ${params.source || 'Claude Code'})` : '';

      return {
        content: [{
          type: 'text',
          text: `Status: ${result.statusCode}\nTime: ${result.time}ms${siteMapInfo}\n\nHeaders:\n${JSON.stringify(result.headers, null, 2)}\n\nBody:\n${result.body}`
        }]
      };
    }
  );
}
