import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { SitemapResult } from '../types/burp.js';

export function registerGetSitemap(server: McpServer, client: BurpClient): void {
  server.tool(
    'get_sitemap',
    'Get discovered endpoints from Burp\'s site map for a domain',
    {
      domain: z.string().optional().describe('Target domain (optional, returns all if not specified)'),
      includeParams: z.boolean().optional().default(true).describe('Include parameter names')
    },
    async (params) => {
      const result = await client.call<SitemapResult>('get_sitemap', params);

      return {
        content: [{
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }]
      };
    }
  );
}
