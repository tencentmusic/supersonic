export enum ChatConfigType {
  DETAIL = 'detail',
  AGG = 'agg',
}

export enum TransType {
  DIMENSION = 'dimension',
  METRIC = 'metric',
}

export enum SemanticNodeType {
  DATASOURCE = 'datasource',
  DIMENSION = 'dimension',
  METRIC = 'metric',
}

export enum MetricTypeWording {
  ATOMIC = '原子指标',
  DERIVED = '衍生指标',
}

export enum DictTaskState {
  ERROR = '错误',
  PENDING = '等待',
  RUNNING = '正在执行',
  SUCCESS = '成功',
  UNKNOWN = '未知',
}

export enum StatusEnum {
  INITIALIZED = 0,
  ONLINE = 1,
  OFFLINE = 2,
  DELETED = 3,
  UNKNOWN = -1,
}

export enum OperatorEnum {
  EQUAL = '=',
  IN = 'IN',
  LIKE = 'LIKE',
}
