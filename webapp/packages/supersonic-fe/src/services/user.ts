import { request } from 'umi';

export async function query() {
  return request<API.CurrentUser[]>(`${process.env.API_BASE_URL}users`);
}

export async function queryCurrentUser() {
  return request<Result<API.CurrentUser>>(`${process.env.AUTH_API_BASE_URL}user/getCurrentUser`);
}
