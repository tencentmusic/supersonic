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

export function changePassword(data: { newPassword: string; oldPassword: string }): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}user/resetPassword`, {
    method: 'post',
    data: {
      newPassword: data.newPassword,
      password: data.oldPassword,
    },
  });
}

// 获取用户accessTokens
export async function getUserAccessTokens(): Promise<Result<API.UserItem[]>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}user/getUserTokens`);
}

export function generateAccessToken(data: { expireTime: number; name: string }): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}user/generateToken`, {
    method: 'post',
    data,
  });
}

export function removeAccessToken(id: number): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}user/deleteUserToken?tokenId=${id}`, {
    method: 'post',
  });
}
