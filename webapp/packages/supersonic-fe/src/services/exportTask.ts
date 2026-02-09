import tRequest from './request';

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
  return tRequest(BASE, { method: 'POST', data });
}

export function getExportTaskList(params?: { current?: number; pageSize?: number }) {
  return tRequest(BASE, { method: 'GET', params });
}

export function getExportTaskById(id: number) {
  return tRequest(`${BASE}/${id}`, { method: 'GET' });
}

export function downloadExportFile(id: number) {
  window.open(`${BASE}/${id}:download`, '_blank');
}

export function cancelExportTask(id: number) {
  return tRequest(`${BASE}/${id}`, { method: 'DELETE' });
}
