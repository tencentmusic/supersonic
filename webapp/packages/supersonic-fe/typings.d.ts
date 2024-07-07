import '@umijs/max/typings';

declare const REACT_APP_ENV: 'test' | 'dev' | 'pre' | false;

declare global {
  namespace NodeJS {
    interface ProcessEnv {
      [key: string]: string | undefined;
      OP: {
        domain: string;
        appId: string;
      };
    }
  }
  interface Window {
    RUNNING_ENV: 'headless' | 'chat';
  }

  type Result<T> = {
    code: number;
    data: T;
    msg: string;
  };

  // 新请求器下的超音数分页接口声明泛型
  type TPaginationResponse<T> = {
    content: (T & AuthSdkType.AuthCodesItem)[];
    current: number;
    pageSize: number;
    total: number;
  };

  type ColumnType = {
    name: string;
    type: string;
  };

  type Pagination = {
    current?: number;
    pageSize?: number;
    sort?: string;
    orderCondition?: string;
  };

  type PromiseSettledList = PromiseSettledItem[];

  type PaginationResponse<T> = Result<{
    content: (T & AuthSdkType.AuthCodesItem)[];
    current: number;
    pageSize: number;
    total: number;
  }>;

  type OptionsItem = {
    value: string | number;
    label: string;
  };
}
