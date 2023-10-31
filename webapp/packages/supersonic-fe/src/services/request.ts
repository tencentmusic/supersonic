import type {
  RequestOptionsInit,
  RequestOptionsWithoutResponse,
  RequestMethod,
  RequestOptionsWithResponse,
  RequestResponse,
  CancelTokenStatic,
  CancelStatic,
} from 'umi-request';
import { extend } from 'umi-request';
import { history } from 'umi';
import { AUTH_TOKEN_KEY } from '@/common/constants';

export const TOKEN_KEY = AUTH_TOKEN_KEY;

const authHeaderInterceptor = (url: string, options: RequestOptionsInit) => {
  const headers: any = {};
  const { query } = history.location as any;

  const token = query[TOKEN_KEY] || localStorage.getItem(TOKEN_KEY);
  if (token) {
    headers.Authorization = `Bearer ${token}`;
    headers.auth = `Bearer ${token}`;
    localStorage.setItem(TOKEN_KEY, token);
  }

  return {
    url,
    options: { ...options, headers },
  };
};

const responseInterceptor = async (response: Response) => {
  const redirect = response.headers.get('redirect'); // 若HEADER中含有REDIRECT说明后端想重定向
  if (redirect === 'REDIRECT') {
    localStorage.removeItem(TOKEN_KEY);
    const win: any = window;
    // 将后端重定向的地址取出来,使用win.location.href去实现重定向的要求
    const contextpath = response.headers.get('contextpath');
    win.location.href = contextpath;
  } else {
    try {
      const data: Result<any> = await response?.clone()?.json?.();
      if (Number(data.code) === 403) {
        history.push('/login');
        return response;
      }
    } catch (e) {}
  }

  return response;
};

let requestMethodInstance: RequestMethod;
const getRequestMethod = () => {
  if (requestMethodInstance) {
    return requestMethodInstance;
  }
  requestMethodInstance = extend({});
  const requestInterceptors = [authHeaderInterceptor];
  const responseInterceptors = [responseInterceptor];
  requestMethodInstance.use(async (ctx, next) => {
    await next();
  });
  requestInterceptors.map((ri) => requestMethodInstance.interceptors.request.use(ri));
  responseInterceptors.map((ri) => requestMethodInstance.interceptors.response.use(ri));
  return requestMethodInstance;
};

const requestMethod = getRequestMethod();

interface RequestMethodInUmi<R = false> {
  <T = any>(url: string, options: RequestOptionsWithResponse): Promise<RequestResponse<T>>;

  <T = any>(url: string, options: RequestOptionsWithoutResponse): Promise<T>;

  <T = any>(url: string, options?: RequestOptionsInit): R extends true
    ? Promise<RequestResponse<T>>
    : Promise<T>;

  get: RequestMethodInUmi<R>;
  post: RequestMethodInUmi<R>;
  delete: RequestMethodInUmi<R>;
  put: RequestMethodInUmi<R>;
  patch: RequestMethodInUmi<R>;
  head: RequestMethodInUmi<R>;
  options: RequestMethodInUmi<R>;
  rpc: RequestMethodInUmi<R>;
  Cancel: CancelStatic;
  CancelToken: CancelTokenStatic;
  isCancel: (value: any) => boolean;
}

// @ts-ignore
const tRequest: RequestMethodInUmi = (url: any, options: any) => {
  return requestMethod(url, options);
};
const METHODS = ['get', 'post', 'delete', 'put', 'patch', 'head', 'options', 'rpc'];
METHODS.forEach((method) => {
  tRequest[method] = (url: any, options: any) => {
    return requestMethod(url, {
      ...options,
      method,
    });
  };
});
tRequest.Cancel = requestMethod.Cancel;
tRequest.CancelToken = requestMethod.CancelToken;
tRequest.isCancel = requestMethod.isCancel;

export default tRequest;

export const request = {
  requestInterceptors: [authHeaderInterceptor],
  responseInterceptors: [responseInterceptor],
};
