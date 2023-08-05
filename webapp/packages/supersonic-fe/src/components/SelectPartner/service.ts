import { request } from 'umi';

export async function getDepartmentTree() {
  return request<any>('/api/tpp/getDetpartmentTree', {
    method: 'GET',
  });
}

export async function getUserByDeptid(id: any) {
  return request<any>(`/api/tpp/getUserByDeptid/${id}`, {
    method: 'GET',
  });
}
