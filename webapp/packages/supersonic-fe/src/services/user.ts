import request from 'umi-request';

export async function query() {
  return request<API.CurrentUser[]>(`${process.env.API_BASE_URL}users`);
}

export async function queryCurrentUser() {
  return request<Result<API.CurrentUser>>(`${process.env.AUTH_API_BASE_URL}user/getCurrentUser`);
}

export function getSystemConfig(): Promise<any> {
  return request(`${process.env.API_BASE_URL}parameter`, {
    method: 'get',
  });
}

export function saveSystemConfig(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}parameter`, {
    method: 'post',
    data,
  });
}
