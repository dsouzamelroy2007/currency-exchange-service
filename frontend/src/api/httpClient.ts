import type { ApiError, ErrorDetails } from './types';

const API_BASE = import.meta.env.VITE_API_BASE ?? '/btc';

async function parseErrorBody(res: Response): Promise<ApiError> {
  const rawBody = await res.text();
  let details: ErrorDetails | null = null;
  try {
    details = JSON.parse(rawBody) as ErrorDetails;
  } catch {
    details = null;
  }

  if (res.status === 502) {
    return { kind: 'gateway-timeout', rawBody };
  }
  return { kind: 'http', status: res.status, details, rawBody };
}

export async function apiGet<T>(path: string, params: Record<string, string>): Promise<T> {
  const query = new URLSearchParams(params).toString();
  const url = `${API_BASE}${path}${query ? `?${query}` : ''}`;

  let res: Response;
  try {
    res = await fetch(url);
  } catch (cause) {
    throw { kind: 'network', cause } satisfies ApiError;
  }

  if (!res.ok) {
    throw await parseErrorBody(res);
  }

  return (await res.json()) as T;
}
