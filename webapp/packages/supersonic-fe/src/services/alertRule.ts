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

export type ResolutionStatus = 'OPEN' | 'CONFIRMED' | 'ASSIGNED' | 'RESOLVED' | 'CLOSED';

export interface AlertEvent {
  id: number;
  executionId: number;
  ruleId: number;
  conditionIndex: number;
  severity: string;
  alertKey: string;
  dimensionValue?: string;
  metricValue?: number;
  baselineValue?: number;
  deviationPct?: number;
  message?: string;
  deliveryStatus: string;
  silenceUntil?: string;
  resolutionStatus: ResolutionStatus;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  assigneeId?: number;
  assignedAt?: string;
  resolvedBy?: string;
  resolvedAt?: string;
  closedAt?: string;
  notes?: string;
  createdAt?: string;
}

export function getEvents(
  params?: {
    current?: number;
    pageSize?: number;
    ruleId?: number;
    severity?: string;
    deliveryStatus?: string;
    resolutionStatus?: string;
  },
): Promise<any> {
  return request(`${BASE}/events`, { method: 'GET', params });
}

export function getEventById(eventId: number): Promise<AlertEvent> {
  return request(`${BASE}/events/${eventId}`, { method: 'GET' });
}

export function transitionEvent(
  eventId: number,
  data: { targetStatus: ResolutionStatus; assigneeId?: number; notes?: string },
): Promise<AlertEvent> {
  return request(`${BASE}/events/${eventId}:transition`, { method: 'POST', data });
}

export function getPendingEventCounts(): Promise<Record<number, number>> {
  return request(`${BASE}/events/pendingCounts`, { method: 'GET' });
}
