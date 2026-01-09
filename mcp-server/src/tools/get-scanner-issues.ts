import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { BurpClient } from '../burp-client.js';
import { ScannerIssuesResult, ScannerIssue } from '../types/burp.js';

export function registerGetScannerIssues(server: McpServer, client: BurpClient): void {
  server.tool(
    'get_scanner_issues',
    'Get vulnerability issues identified by the Burp Scanner (Burp Professional only)',
    {
      limit: z.number().optional().default(100).describe('Maximum number of issues to return'),
      offset: z.number().optional().default(0).describe('Number of issues to skip (for pagination)'),
      url: z.string().optional().describe('Filter issues by URL (partial match)'),
      severity: z.enum(['HIGH', 'MEDIUM', 'LOW', 'INFORMATION']).optional().describe('Filter by severity level')
    },
    async (params) => {
      const result = await client.call<ScannerIssuesResult>('get_scanner_issues', params);

      if (result.issues.length === 0) {
        return {
          content: [{
            type: 'text',
            text: `No scanner issues found. Total issues in site map: ${result.total}`
          }]
        };
      }

      const issuesSummary = result.issues.map((issue: ScannerIssue) => formatIssue(issue)).join('\n\n---\n\n');

      return {
        content: [{
          type: 'text',
          text: `Found ${result.returned} issues (${result.total} total, offset: ${result.offset}):\n\n${issuesSummary}`
        }]
      };
    }
  );
}

function formatIssue(issue: ScannerIssue): string {
  const lines: string[] = [
    `**${issue.name}**`,
    `Severity: ${issue.severity} | Confidence: ${issue.confidence}`,
    `URL: ${issue.baseUrl}`
  ];

  if (issue.httpService) {
    lines.push(`Host: ${issue.httpService.host}:${issue.httpService.port} (${issue.httpService.secure ? 'HTTPS' : 'HTTP'})`);
  }

  if (issue.detail) {
    lines.push(`\nDetail: ${issue.detail}`);
  }

  if (issue.remediation) {
    lines.push(`\nRemediation: ${issue.remediation}`);
  }

  if (issue.definition?.background) {
    lines.push(`\nBackground: ${issue.definition.background}`);
  }

  if (issue.requestResponses && issue.requestResponses.length > 0) {
    lines.push(`\nEvidence (${issue.requestResponses.length} request/response pairs available)`);
  }

  if (issue.collaboratorInteractions && issue.collaboratorInteractions.length > 0) {
    lines.push(`\nCollaborator interactions: ${issue.collaboratorInteractions.length}`);
  }

  return lines.join('\n');
}
