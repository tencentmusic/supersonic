import tRequest from './request';

const BASE = '/api/v1/reportSchedules';

export interface ReportSchedule {
  id: number;
  name: string;
  datasetId: number;
  queryConfig?: string;
  outputFormat: string;
  cronExpression: string;
  enabled: boolean;
  ownerId?: number;
  retryCount: number;
  retryInterval: number;
  templateVersion?: number;
  deliveryConfigIds?: string;
  quartzJobKey?: string;
  lastExecutionTime?: string;
  nextExecutionTime?: string;
  createdAt?: string;
  createdBy?: string;
  tenantId?: number;
}

export interface ReportExecution {
  id: number;
  scheduleId?: number;
  attempt: number;
  status: string;
  startTime?: string;
  endTime?: string;
  resultLocation?: string;
  errorMessage?: string;
  rowCount?: number;
  sqlHash?: string;
  executionTimeMs?: number;
  scanRows?: number;
}

export function getScheduleList(params: {
  current?: number;
  pageSize?: number;
  datasetId?: number;
  enabled?: boolean;
}) {
  return tRequest(BASE, { method: 'GET', params });
}

export function getScheduleById(id: number) {
  return tRequest(`${BASE}/${id}`, { method: 'GET' });
}

export function createSchedule(data: Partial<ReportSchedule>) {
  return tRequest(BASE, { method: 'POST', data });
}

export function updateSchedule(id: number, data: Partial<ReportSchedule>) {
  return tRequest(`${BASE}/${id}`, { method: 'PATCH', data });
}

export function deleteSchedule(id: number) {
  return tRequest(`${BASE}/${id}`, { method: 'DELETE' });
}

export function pauseSchedule(id: number) {
  return tRequest(`${BASE}/${id}:pause`, { method: 'POST' });
}

export function resumeSchedule(id: number) {
  return tRequest(`${BASE}/${id}:resume`, { method: 'POST' });
}

export function triggerSchedule(id: number) {
  return tRequest(`${BASE}/${id}:trigger`, { method: 'POST' });
}

export function getExecutionList(
  scheduleId: number,
  params?: { current?: number; pageSize?: number; status?: string },
) {
  return tRequest(`${BASE}/${scheduleId}/executions`, { method: 'GET', params });
}

export function getExecutionById(scheduleId: number, executionId: number) {
  return tRequest(`${BASE}/${scheduleId}/executions/${executionId}`, { method: 'GET' });
}

export function downloadExecutionResult(scheduleId: number, executionId: number) {
  window.open(`${BASE}/${scheduleId}/executions/${executionId}:download`, '_blank');
}
