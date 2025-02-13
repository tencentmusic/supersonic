declare module 'slash2';
declare module '*.css';
declare module '*.less';
declare module '*.module.less';
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
declare module 'mockjs';
declare module 'react-fittext';
declare module 'bizcharts-plugin-slider';
declare module 'react-split-pane/lib/Pane';

declare const REACT_APP_ENV: 'test' | 'dev' | 'pre' | false;

declare module '*.module.less' {
  const classes: {
    readonly [key: string]: string
  }
  export default classes
  declare module '*.less'
}

interface AxiosResponse<T = any> extends Promise<T> {
  code: number;
  data: T;
  msg: string;  
}

type Result<T> = {
  code: number;
  data: T;
  msg: string;
};

type DavinciResponseHeader = {
  code: number;
  msg: string;
  token: string;
};

type DavinciResponse<T> = {
  header: DavinciResponseHeader;
  payload: T;
};

// 达芬奇接口返回的参数格式
type DavinciResult<T> = {
  payload: T;
  header: {
    msg: string;
    code: number;
    token: string;
  };
};

// 新请求器下的超音数分页接口声明泛型
type TPaginationResponse<T> = {
  content: T[];
  current: number;
  pageSize: number;
  total: number;
};

type DavinciPaginationResponse<T> = DavinciResult<{
  resultList: T[];
  pageNo: number;
  pageSize: number;
  totalCount: number;
  [key: string]: any;
}>;

type BDResponse<T> = {
  code: string;
  data: T;
  msg: string;
  traceId: string;
};

type TopNConfig = {
  computeType: 'field' | 'dimension';
  column: string;
  direction: 'asc' | 'desc';
  limit: number;
};

type ColumnType = {
  name: string;
  nameEn: string;
  type: string;
};

type DataType = {
  columns: ColumnType[];
  pageNo: number;
  pageSize: number;
  totalCount: number;
  resultList: any[];
  sqlToExec: string;
  timeUsed: number;
};

type QueryVariable = { name: string; value: string | number }[];

type GetDataParams = {
  groups: string[];
  aggregators: { column: string; func: string }[];
  filters: any[];
  params?: QueryVariable;
  orders?: { column: string; direction?: string; sortList?: string[] }[];
  limit: number;
  cache: boolean;
  expired: number;
  flush: boolean;
  pageNo?: number;
  pageSize?: number;
  nativeQuery: boolean;
  topN?: TopNConfig;
  classId?: number;
};

type ReportEventParams = {
  event: string;
  dt_pgid?: string;
  page_title: string;
  page_path?: string;
  entity_id?: string | number;
  singer_id?: number;
  producer?: string;
  ip?: string;
  song_id?: number;
  album_id?: number;
  brand_id?: number;
  company_id?: number;
  song_ids?: string;
  compare_Ids?: string;
  element_name?: string;
  entrance_name?: string;
  category_id?: string;
  category_type?: string;
  conversation_name?: string;
  msg?: string;
  msg_type?: string;
  search_value?: string;
  [key: string]: string | number;
};

type RowSpanMapIndexItem = number[];
type RowSpanMap = Record<string, RowSpanMapIndexItem>;

type Pagination = {
  current?: number;
  pageSize?: number;
  sort?: string;
  orderCondition?: string;
};

type PromiseSettledItem = {
  status: string;
  value?: any;
  reason?: any;
};

type PromiseSettledList = PromiseSettledItem[];

type PaginationResponse<T> = Result<{
  content: T[];
  current: number;
  pageSize: number;
  total: number;
}>;
