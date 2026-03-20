import request from './request';

const BASE = '/api/v1/alertRules';

export interface AlertRule {
  id?: number;
  name: string;
  description?: string;
  datasetId: number;
  queryConfig?: string;
  conditions?: string;
  cronExpression?: string;
  enabled?: number;
  ownerId?: number;
  deliveryConfigIds?: string;
  silenceMinutes?: number;
  maxConsecutiveFailures?: number;
  retryCount?: number;
  retryInterval?: number;
  lastCheckTime?: string;
  nextCheckTime?: string;
  createdAt?: string;
  createdBy?: string;
  tenantId?: number;
}

export interface AlertExecution {
  id: number;
  ruleId: number;
  status: string;
  startTime?: string;
  endTime?: string;
  totalRows?: number;
  alertedRows?: number;
  silencedRows?: number;
  errorMessage?: string;
  executionTimeMs?: number;
  createdAt?: string;
}

export function getRuleList(params?: {
  current?: number;
  pageSize?: number;
  datasetId?: number;
  enabled?: boolean;
}) {
  return request(BASE, { method: 'GET', params });
}

export function getRuleById(id: number) {
  return request(`${BASE}/${id}`, { method: 'GET' });
}

export function createRule(data: Partial<AlertRule>) {
  return request(BASE, { method: 'POST', data });
}

export function updateRule(id: number, data: Partial<AlertRule>) {
  return request(`${BASE}/${id}`, { method: 'PATCH', data });
}

export function deleteRule(id: number) {
  return request(`${BASE}/${id}`, { method: 'DELETE' });
}

export function pauseRule(id: number) {
  return request(`${BASE}/${id}:pause`, { method: 'POST' });
}

export function resumeRule(id: number) {
  return request(`${BASE}/${id}:resume`, { method: 'POST' });
}

export function triggerRule(id: number) {
  return request(`${BASE}/${id}:trigger`, { method: 'POST' });
}

export function testRule(id: number) {
  return request(`${BASE}/${id}:test`, { method: 'POST' });
}

export function getExecutions(
  ruleId: number,
  params?: { current?: number; pageSize?: number; status?: string },
) {
  return request(`${BASE}/${ruleId}/executions`, { method: 'GET', params });
}
