export const EDITOR_HEIGHT_MAP = new Map([
  ['small', '250px'],
  ['middle', '300px'],
  ['large', '400px'],
]);

export enum EnumDataSourceType {
  CATEGORICAL = 'categorical',
  TIME = 'time',
  PARTITION_TIME = 'partition_time',
  MEASURES = 'measures',
  PRIMARY = 'primary',
  FOREIGN = 'foreign',
  PRIMARY_KEY = 'primary_key',
  FOREIGN_KEY = 'foreign_key',
  IDENTIFY = 'identify',
}

export enum EnumModelDataType {
  DIMENSION = 'dimension',
  IDENTIFIERS = 'identifiers',
  MEASURES = 'measures',
}

export const modelDataClass = {
  [EnumModelDataType.DIMENSION]: [
    EnumDataSourceType.CATEGORICAL,
    EnumDataSourceType.TIME,
    EnumDataSourceType.PARTITION_TIME,
  ],
  [EnumModelDataType.IDENTIFIERS]: [EnumDataSourceType.PRIMARY, EnumDataSourceType.FOREIGN],
  [EnumModelDataType.MEASURES]: [EnumDataSourceType.MEASURES],
};

export const DIM_OPTIONS = [
  // {
  //   label: '主键',
  //   value: EnumDataSourceType.PRIMARY_KEY,
  // },
  // {
  //   label: '外键',
  //   value: EnumDataSourceType.FOREIGN_KEY,
  // },
  {
    label: '枚举',
    value: EnumDataSourceType.CATEGORICAL,
  },
  {
    label: '普通时间',
    value: EnumDataSourceType.TIME,
  },
  {
    label: '数据时间',
    value: EnumDataSourceType.PARTITION_TIME,
  },
];

export const TYPE_OPTIONS_LABEL: Record<string, string> = {
  [EnumModelDataType.DIMENSION]: '枚举',
  [EnumModelDataType.MEASURES]: '度量',
  [EnumDataSourceType.PRIMARY]: '主键',
  [EnumDataSourceType.FOREIGN]: '外键',
};

export const TYPE_OPTIONS = [
  {
    label: '维度',
    value: EnumModelDataType.DIMENSION,
  },
  // {
  //   label: '日期',
  //   value: EnumDataSourceType.TIME,
  // },
  {
    label: '度量',
    value: EnumDataSourceType.MEASURES,
  },
  {
    label: '主键',
    value: EnumDataSourceType.PRIMARY,
  },
  {
    label: '外键',
    value: EnumDataSourceType.FOREIGN,
  },
];

export const AGG_OPTIONS = [
  {
    label: 'sum',
    value: 'sum',
  },
  {
    label: 'max',
    value: 'max',
  },
  {
    label: 'min',
    value: 'min',
  },
  {
    label: 'avg',
    value: 'avg',
  },
  {
    label: 'count',
    value: 'count',
  },
  {
    label: 'count_distinct',
    value: 'count_distinct',
  },
  {
    label: 'none',
    value: '',
  },
];

export const DATE_OPTIONS = ['day', 'week', 'month'];

export const DATE_FORMATTER = [
  'yyyy-MM-dd',
  'yyyy-MM-dd HH:mm:ss',
  'yyyy-MM-dd HH:mm',
  'yyyy-MM-dd HH',
  'yyyyMMdd',
  'yyyy-MM',
  'yyyyMM',
];

export const PARTITION_TIME_FORMATTER = [
  'yyyy-MM-dd',
  'yyyy-MM-dd HH',
  'yyyyMMdd',
  'yyyy-MM',
  'yyyyMM',
];
