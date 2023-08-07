import request from 'umi-request';

export interface UserItem {
  id: number;
  name: string;
  displayName: string;
  email: string;
}

export type GetAllUserRes = Result<UserItem[]>;

// 获取所有用户
export async function getAllUser(): Promise<GetAllUserRes> {
  return request.get(`${process.env.AUTH_API_BASE_URL}user/getUserList`);
}
