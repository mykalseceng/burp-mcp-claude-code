import WebSocket from 'ws';
import { RpcRequest, RpcResponse } from './types/rpc.js';

interface PendingRequest {
  resolve: (value: unknown) => void;
  reject: (error: Error) => void;
  timeout: NodeJS.Timeout;
}

export class BurpClient {
  private ws: WebSocket | null = null;
  private url: string;
  private authToken: string;
  private pendingRequests: Map<string, PendingRequest> = new Map();
  private requestId = 0;
  private reconnectDelay: number;
  private connected = false;

  constructor(options: { url: string; authToken?: string; reconnectDelay?: number }) {
    this.url = options.url;
    this.authToken = options.authToken || '';
    this.reconnectDelay = options.reconnectDelay || 5000;
  }

  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      const headers: Record<string, string> = {};
      if (this.authToken) {
        headers['Authorization'] = `Bearer ${this.authToken}`;
      }

      this.ws = new WebSocket(this.url, { headers });

      this.ws.on('open', () => {
        this.connected = true;
        console.error('[BurpClient] Connected to Burp extension');
        resolve();
      });

      this.ws.on('message', (data: WebSocket.Data) => {
        this.handleMessage(data.toString());
      });

      this.ws.on('close', () => {
        this.connected = false;
        console.error('[BurpClient] Disconnected from Burp extension');
        this.scheduleReconnect();
      });

      this.ws.on('error', (error) => {
        console.error('[BurpClient] WebSocket error:', error.message);
        if (!this.connected) {
          reject(error);
        }
      });
    });
  }

  private scheduleReconnect(): void {
    setTimeout(() => {
      console.error('[BurpClient] Attempting to reconnect...');
      this.connect().catch(() => {});
    }, this.reconnectDelay);
  }

  private handleMessage(data: string): void {
    try {
      const response: RpcResponse = JSON.parse(data);
      const pending = this.pendingRequests.get(response.id);

      if (pending) {
        clearTimeout(pending.timeout);
        this.pendingRequests.delete(response.id);

        if (response.error) {
          pending.reject(new Error(`${response.error.code}: ${response.error.message}`));
        } else {
          pending.resolve(response.result);
        }
      }
    } catch (error) {
      console.error('[BurpClient] Failed to parse response:', error);
    }
  }

  async call<T>(method: string, params: Record<string, unknown> = {}): Promise<T> {
    if (!this.connected || !this.ws) {
      throw new Error('Not connected to Burp extension');
    }

    const id = String(++this.requestId);
    const request: RpcRequest = {
      jsonrpc: '2.0',
      id,
      method,
      params
    };

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pendingRequests.delete(id);
        reject(new Error('Request timed out'));
      }, 30000);

      this.pendingRequests.set(id, { resolve: resolve as (v: unknown) => void, reject, timeout });
      this.ws!.send(JSON.stringify(request));
    });
  }

  isConnected(): boolean {
    return this.connected;
  }

  close(): void {
    if (this.ws) {
      this.ws.close();
    }
  }
}
