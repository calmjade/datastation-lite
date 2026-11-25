import type {
  HealthStatus,
  QueryExecuteResponse,
  ReportConfig,
  ReportRequest,
  ReportRunResponse,
  TableMeta,
} from '../types/datastation';

export class ApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers || {}),
    },
    ...init,
  });

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!res.ok) {
    const errObj = (data && typeof data === 'object' ? data : {}) as Record<string, unknown>;
    const message =
      (typeof errObj.message === 'string' && errObj.message) ||
      `请求失败 (${res.status})`;
    const code = (typeof errObj.error === 'string' && errObj.error) || 'API_ERROR';
    throw new ApiError(res.status, code, message);
  }

  return data as T;
}

export function fetchHealth(): Promise<HealthStatus> {
  return requestJson<HealthStatus>('/api/health');
}

export function fetchSchemaTables(): Promise<TableMeta[]> {
  return requestJson<TableMeta[]>('/api/schema/tables');
}

export function executeQuery(sql: string, limit = 200): Promise<QueryExecuteResponse> {
  return requestJson<QueryExecuteResponse>('/api/query/execute', {
    method: 'POST',
    body: JSON.stringify({ sql, limit }),
  });
}

export function fetchReports(): Promise<ReportConfig[]> {
  return requestJson<ReportConfig[]>('/api/reports');
}

export function fetchReport(id: number): Promise<ReportConfig> {
  return requestJson<ReportConfig>(`/api/reports/${id}`);
}

export function createReport(payload: ReportRequest): Promise<ReportConfig> {
  return requestJson<ReportConfig>('/api/reports', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateReport(id: number, payload: ReportRequest): Promise<ReportConfig> {
  return requestJson<ReportConfig>(`/api/reports/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteReport(id: number): Promise<void> {
  return requestJson<void>(`/api/reports/${id}`, {
    method: 'DELETE',
  });
}

export function runReport(id: number, limit = 500): Promise<ReportRunResponse> {
  return requestJson<ReportRunResponse>(`/api/reports/${id}/run`, {
    method: 'POST',
    body: JSON.stringify({ limit }),
  });
}
