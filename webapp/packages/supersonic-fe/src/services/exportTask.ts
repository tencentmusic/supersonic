import request from './request';

const BASE = '/api/v1/exportTasks';

export interface ExportTask {
  id: number;
  taskName?: string;
  userId: number;
  datasetId?: number;
  queryConfig?: string;
  outputFormat: string;
  status: string;
  fileLocation?: string;
  fileSize?: number;
  rowCount?: number;
  errorMessage?: string;
  createdAt?: string;
  expireTime?: string;
}

export function submitExportTask(data: Partial<ExportTask>) {
  return request(BASE, { method: 'POST', data });
}

export function getExportTaskList(params?: { current?: number; pageSize?: number }) {
  return request(BASE, { method: 'GET', params });
}

export function getExportTaskById(id: number) {
  return request(`${BASE}/${id}`, { method: 'GET' });
}

export function downloadExportFile(id: number) {
  window.open(`${BASE}/${id}:download`, '_blank');
}

export function cancelExportTask(id: number) {
  return request(`${BASE}/${id}`, { method: 'DELETE' });
}
