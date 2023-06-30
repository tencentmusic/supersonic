import { request } from 'umi';

export type LoginParamsType = {
  username: string;
  password: string;
  mobile: string;
  captcha: string;
  type: string;
};

export async function queryToken(code: string) {
  return request(`/davinciapi/login/tmeloginCallback`, {
    params: { code },
  });
}
