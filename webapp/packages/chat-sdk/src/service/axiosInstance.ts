// 引入axios库
import axios, { AxiosInstance } from 'axios';
import { getToken } from '../utils/utils';

// 创建axios实例
const axiosInstance: AxiosInstance = axios.create({
  // 设置基本URL，所有请求都会使用这个URL作为前缀
  baseURL: '',
  // 设置请求超时时间（毫秒）
  timeout: 120000,
  // 设置请求头
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
axiosInstance.interceptors.request.use(
  (config: any) => {
    const token = getToken();
    if (token && config?.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    // 请求错误时的处理
    return Promise.reject(error);
  }
);

// 响应拦截器
axiosInstance.interceptors.response.use(
  (response: any) => {
    const redirect = response.headers['redirect'];
    if (redirect === 'REDIRECT') {
      let win: any = window;
      while (win !== win.top) {
        win = win.top;
      }
      const contextpath = response.headers['contextpath'];
      win.location.href =
        contextpath?.substring(0, contextpath?.indexOf('&')) +
        `&redirect_uri=${encodeURIComponent(`http://${win.location.host}`)}`;
    }
    return response.data;
  },
  (error) => {
    // 对响应错误进行处理
    if (error.response && error.response.status === 401) {
      // 如果响应状态码为401，表示未授权，可以在这里处理重新登录等操作
      console.log('Unauthorized, please log in again.');
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;