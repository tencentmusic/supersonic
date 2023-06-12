import request from 'umi-request';

export interface PostUserLoginRes {
  code: string; // 返回编码
  msg: string; // 返回消息
  data: string;
  traceId: string;
}

export interface PostUserRegesiterRes {
  code: string; // 返回编码
  msg: string; // 返回消息
  data: never;
  traceId: string;
}

export function userRegister(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}user/register`, {
    method: 'POST',
    data,
  });
}

export function postUserLogin(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}user/login`, {
    method: 'POST',
    data,
  });
}
