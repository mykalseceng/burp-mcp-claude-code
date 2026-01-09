export interface StoredRequest {
  id: number;
  timestamp: number;
  method: string;
  url: string;
  host: string;
  port: number;
  isHttps: boolean;
  requestHeaders: Record<string, string>;
  requestBody: string;
  statusCode: number;
  responseHeaders: Record<string, string>;
  responseBody: string;
  mimeType: string;
  toolSource: string;
}

export interface SitemapEntry {
  url: string;
  method: string;
  statusCode: number;
  mimeType: string;
  parameters?: string[];
}

export interface ProxyHistoryResult {
  requests: StoredRequest[];
  total: number;
  returned: number;
}

export interface SitemapResult {
  entries: SitemapEntry[];
  count: number;
}

export interface SendRequestResult {
  statusCode: number;
  headers: Record<string, string>;
  body: string;
  time: number;
  addedToSiteMap: boolean;
}

export interface ScanResult {
  scanId: string;
  status: string;
  message: string;
  crawlEnabled?: boolean;
}

export interface StopScanResult {
  scanId: string;
  stopped: boolean;
  crawlStopped: boolean;
  auditStopped: boolean;
  targetUrl: string;
  message: string;
}

export interface ScannerIssue {
  name: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW' | 'INFORMATION' | 'FALSE_POSITIVE';
  confidence: 'CERTAIN' | 'FIRM' | 'TENTATIVE';
  baseUrl: string;
  detail?: string;
  remediation?: string;
  httpService?: {
    host: string;
    port: number;
    secure: boolean;
  };
  definition?: {
    name: string;
    background?: string;
    remediation?: string;
    typeIndex: number;
  };
  requestResponses?: Array<{
    request?: string;
    response?: string;
  }>;
  collaboratorInteractions?: Array<{
    id: string;
    timestamp: string;
  }>;
}

export interface ScannerIssuesResult {
  issues: ScannerIssue[];
  total: number;
  returned: number;
  offset: number;
}
