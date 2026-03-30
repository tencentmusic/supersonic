import request from './request';

const BASE = '/api/v1/reportSchedules';
const CHAT_CONF_BASE = '/api/chat/conf';

/** 有效数据集（供调度关联选择），仅包含系统已配置且状态为 ONLINE/OFFLINE 的数据集 */
export interface ValidDataSetItem {
  id: number;
  name: string;
  domainId?: number;
  partitionDimension?: string;
}

export interface DataSetSchemaField {
  id: number;
  name: string;
  bizName: string;
}

export function getValidDataSetList(): Promise<ValidDataSetItem[]> {
  return request(`${process.env.API_BASE_URL || ''}dataSet/getValidDataSetList`, { method: 'GET' });
}

/** Returns DataSetSchema with dimensions: SchemaElement[] */
export function getDataSetSchema(dataSetId: number): Promise<any> {
  return request(`${CHAT_CONF_BASE}/getDataSetSchema/${dataSetId}`, { method: 'GET' });
}

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
  return request(BASE, { method: 'GET', params });
}

export function getScheduleById(id: number) {
  return request(`${BASE}/${id}`, { method: 'GET' });
}

export function createSchedule(data: Partial<ReportSchedule>) {
  return request(BASE, { method: 'POST', data });
}

export function updateSchedule(id: number, data: Partial<ReportSchedule>) {
  return request(`${BASE}/${id}`, { method: 'PATCH', data });
}

export function deleteSchedule(id: number) {
  return request(`${BASE}/${id}`, { method: 'DELETE' });
}

export function pauseSchedule(id: number) {
  return request(`${BASE}/${id}:pause`, { method: 'POST' });
}

export function resumeSchedule(id: number) {
  return request(`${BASE}/${id}:resume`, { method: 'POST' });
}

export function triggerSchedule(id: number) {
  return request(`${BASE}/${id}:trigger`, { method: 'POST' });
}

export function getExecutionList(
  scheduleId: number,
  params?: { current?: number; pageSize?: number; status?: string },
) {
  return request(`${BASE}/${scheduleId}/executions`, { method: 'GET', params });
}

export function getExecutionById(scheduleId: number, executionId: number) {
  return request(`${BASE}/${scheduleId}/executions/${executionId}`, { method: 'GET' });
}

export function downloadExecutionResult(scheduleId: number, executionId: number) {
  window.open(`${BASE}/${scheduleId}/executions/${executionId}:download`, '_blank');
}
