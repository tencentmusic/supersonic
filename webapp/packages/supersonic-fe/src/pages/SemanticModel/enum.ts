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
  error = '错误',
  pending = '等待',
  running = '正在执行',
  success = '成功',
  unknown = '未知',
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

export enum KnowledgeConfigTypeEnum {
  DIMENSION = 'DIMENSION',
  METRIC = 'METRIC',
  DOMAIN = 'DOMAIN',
  ENTITY = 'ENTITY',
  VIEW = 'VIEW',
  MODEL = 'MODEL',
  UNKNOWN = 'UNKNOWN',
}

export enum KnowledgeConfigStatusEnum {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
  DELETED = 'DELETED',
  INITIALIZED = 'INITIALIZED',
  UNAVAILABLE = 'UNAVAILABLE',
  UNKNOWN = 'UNKNOWN',
}
