export enum ChatConfigType {
  TAG = 'TAG',
  METRIC = 'METRIC',
}

export enum TransType {
  DIMENSION = 'DIMENSION',
  METRIC = 'METRIC',
}

export enum SemanticNodeType {
  DATASOURCE = 'DATASOURCE',
  DIMENSION = 'DIMENSION',
  METRIC = 'METRIC',
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
  UNKNOWN = -1,
  INITIALIZED = 0,
  ONLINE = 1,
  OFFLINE = 2,
  DELETED = 3,
  UNAVAILABLE = 4,
}

export enum OperatorEnum {
  EQUAL = '=',
  IN = 'IN',
  LIKE = 'LIKE',
}
