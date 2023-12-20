declare module 'slash2';
declare module '*.css';
declare module '*.less';
declare module '*.scss';
declare module '*.sass';
declare module '*.svg';
declare module '*.png';
declare module '*.jpg';
declare module '*.jpeg';
declare module '*.gif';
declare module '*.bmp';
declare module '*.tiff';
declare module 'omit.js';
declare module 'numeral';
declare module '@antv/data-set';
declare module 'react-fittext';
declare module 'bizcharts-plugin-slider';
declare module 'react-split-pane/lib/Pane';

declare const REACT_APP_ENV: 'test' | 'dev' | 'pre' | false;

declare interface Window {
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
