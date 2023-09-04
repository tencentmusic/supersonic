export const EDITOR_HEIGHT_MAP = new Map([
  ['small', '250px'],
  ['middle', '300px'],
  ['large', '400px'],
]);

export enum EnumDataSourceType {
  CATEGORICAL = 'categorical',
  TIME = 'time',
  MEASURES = 'measures',
  PRIMARY = 'primary',
  FOREIGN = 'foreign',
}

export const TYPE_OPTIONS = [
  {
    label: '维度',
    value: EnumDataSourceType.CATEGORICAL,
  },
  {
    label: '日期',
    value: EnumDataSourceType.TIME,
  },
  {
    label: '度量',
    value: EnumDataSourceType.MEASURES,
  },
  {
    label: '主键',
    value: EnumDataSourceType.PRIMARY,
  },
  // {
  //   label: '外键',
  //   value: EnumDataSourceType.FOREIGN,
  // },
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
];

export const DATE_OPTIONS = ['day', 'week', 'month'];

export const DATE_FORMATTER = ['YYYY-MM-DD', 'YYYYMMDD', 'YYYY-MM', 'YYYYMM'];
