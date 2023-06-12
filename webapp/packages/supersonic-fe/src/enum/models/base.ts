export const EnumTransDbType = {
  mysql: 'mysql',
  tdw: 'tdw',
  clickhouse: 'clickhouse',
  kafka: 'kafka',
  binlog: 'binlog',
  hbase: 'hbase',
  kugou_datahub: 'kugou_datahub',
  aiting_datahub: 'aiting_datahub',
  http: 'http',
};

export const EnumTransModelType = {
  edit: '编辑',
  add: '新增',
};

export const EnumDescSensitivity = {
  low: {
    value: 1,
    label: '低',
  },
  middle: {
    value: 2,
    label: '中',
  },
  height: {
    value: 3,
    label: '高',
  },
};

export const EnumDbTypeOwnKeys = {
  mysql: ['ip', 'port', 'dbName', 'username', 'password'],
  clickhouse: ['ip', 'port', 'dbName', 'username', 'password'],
  tdw: ['dbName', 'username', 'password'],
  kafka: ['bootstrap', 'dbName', 'username', 'password'],
  binlog: ['ip', 'port', 'dbName', 'username', 'password'],
  hbase: ['config'],
  kugou_datahub: ['config'],
  aiting_datahub: ['config'],
  http: ['url'],
};

export enum EnumDashboardType {
  DIR = 0, // 目录
  DASHBOARD = 1, // 看板
}
