import request from './request';

const MAPPING_URL = '/api/v1/feishu/userMappings';
const SESSION_URL = '/api/v1/feishu/querySessions';

// ============ User Mapping ============
export async function getFeishuMappings(params: {
  current?: number;
  pageSize?: number;
}) {
  return request(`${MAPPING_URL}`, {
    method: 'GET',
    params,
  });
}

export async function getFeishuMappingById(id: number) {
  return request(`${MAPPING_URL}/${id}`, {
    method: 'GET',
  });
}

export async function createFeishuMapping(data: any) {
  return request(`${MAPPING_URL}`, {
    method: 'POST',
    data,
  });
}

export async function updateFeishuMapping(id: number, data: any) {
  return request(`${MAPPING_URL}/${id}`, {
    method: 'PATCH',
    data,
  });
}

export async function deleteFeishuMapping(id: number) {
  return request(`${MAPPING_URL}/${id}`, {
    method: 'DELETE',
  });
}

export async function enableFeishuMapping(id: number) {
  return request(`${MAPPING_URL}/${id}:enable`, {
    method: 'POST',
  });
}

export async function disableFeishuMapping(id: number) {
  return request(`${MAPPING_URL}/${id}:disable`, {
    method: 'POST',
  });
}

// ============ Query Sessions ============
export async function getFeishuSessions(params: {
  current?: number;
  pageSize?: number;
  status?: string;
  startDate?: string;
  endDate?: string;
  scope?: 'self' | 'tenant';
}) {
  return request(`${SESSION_URL}`, {
    method: 'GET',
    params,
  });
}
