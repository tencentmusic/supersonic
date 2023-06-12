import { request } from 'umi';

export type LoginParamsType = {
  username: string;
  password: string;
  mobile: string;
  captcha: string;
  type: string;
};

export async function queryToken(code: string) {
  return request(`${process.env.API_BASE_URL}user/ioaLoginCallback`, {
    params: { code },
  });
}
