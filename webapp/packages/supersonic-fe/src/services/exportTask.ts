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

function parseFilenameFromContentDisposition(contentDisposition?: string | null, fallback?: string) {
  if (!contentDisposition) {
    return fallback || 'export.xlsx';
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }
  const basicMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
  if (basicMatch?.[1]) {
    return basicMatch[1];
  }
  return fallback || 'export.xlsx';
}

export async function downloadExportFile(id: number, fallbackName?: string) {
  const res = await request(`${BASE}/${id}:download`, {
    method: 'GET',
    responseType: 'blob',
    getResponse: true,
  });
  const blob = res?.data as Blob;
  const contentDisposition = res?.response?.headers?.get?.('content-disposition');
  const fileName = parseFilenameFromContentDisposition(contentDisposition, fallbackName);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

export function cancelExportTask(id: number) {
  return request(`${BASE}/${id}`, { method: 'DELETE' });
}
