import request from '@/services/request';

export async function getUserByDeptid(id: any) {
  return request.get<any>(`${process.env.AUTH_API_BASE_URL}user/getUserByOrg/${id}`);
}
export async function getOrganizationTree() {
  return request.get<any>(`${process.env.AUTH_API_BASE_URL}user/getOrganizationTree`);
}
