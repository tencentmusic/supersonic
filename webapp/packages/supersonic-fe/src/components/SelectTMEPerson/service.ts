import request from 'umi-request';

export type UserItem = {
  enName?: string;
  displayName: string;
  chName?: string;
  name?: string;
  email: string;
};
export type GetAllUserRes = Result<UserItem[]>;

// 获取所有用户
export async function getAllUser(): Promise<GetAllUserRes> {
  const { APP_TARGET } = process.env;
  if (APP_TARGET === 'inner') {
    return request.get('/api/oa/user/all');
  }
  return request.get(`${process.env.AUTH_API_BASE_URL}user/getUserList`);
}
