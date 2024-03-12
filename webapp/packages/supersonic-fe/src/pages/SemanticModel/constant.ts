import { SemanticNodeType } from './enum';
import { DateRangeType } from '@/components/MDatePicker/type';

export enum SENSITIVE_LEVEL {
  LOW = 0,
  MID = 1,
  HIGH = 2,
}

export const SENSITIVE_LEVEL_OPTIONS = [
  {
    label: '普通',
    value: SENSITIVE_LEVEL.LOW,
  },
  {
    label: '重要',
    value: SENSITIVE_LEVEL.MID,
  },
  {
    label: '核心',
    value: SENSITIVE_LEVEL.HIGH,
  },
];

export const SENSITIVE_LEVEL_ENUM = SENSITIVE_LEVEL_OPTIONS.reduce(
  (sensitiveEnum: any, item: any) => {
    const { label, value } = item;
    sensitiveEnum[value] = label;
    return sensitiveEnum;
  },
  {},
);

export const IS_TAG_ENUM = {
  1: '是',
  0: '否',
};

export const SENSITIVE_LEVEL_COLOR = {
  [SENSITIVE_LEVEL.LOW]: 'default',
  [SENSITIVE_LEVEL.MID]: 'orange',
  // [SENSITIVE_LEVEL.MID]: 'geekblue',
  [SENSITIVE_LEVEL.HIGH]: 'volcano',
  // [SENSITIVE_LEVEL.HIGH]: '#1677ff',
};

export const SEMANTIC_NODE_TYPE_CONFIG = {
  [SemanticNodeType.DATASOURCE]: {
    label: '模型',
    value: SemanticNodeType.DATASOURCE,
    color: 'cyan',
  },
  [SemanticNodeType.DIMENSION]: {
    label: '维度',
    value: SemanticNodeType.DIMENSION,
    color: 'blue',
  },
  [SemanticNodeType.METRIC]: {
    label: '指标',
    value: SemanticNodeType.METRIC,
    color: 'orange',
  },
};

export const DateFieldMap = {
  [DateRangeType.DAY]: 'sys_imp_date',
  [DateRangeType.WEEK]: 'sys_imp_week',
  [DateRangeType.MONTH]: 'sys_imp_month',
};

export const DatePeridMap = {
  sys_imp_date: DateRangeType.DAY,
  sys_imp_week: DateRangeType.WEEK,
  sys_imp_month: DateRangeType.MONTH,
};

export enum METRIC_DEFINE_TYPE {
  FIELD = 'FIELD',
  MEASURE = 'MEASURE',
  METRIC = 'METRIC',
}

export enum TAG_DEFINE_TYPE {
  FIELD = 'FIELD',
  DIMENSION = 'DIMENSION',
  METRIC = 'METRIC',
}

export const TagDefineTypeMap = {
  [TAG_DEFINE_TYPE.FIELD]: '字段',
  [TAG_DEFINE_TYPE.DIMENSION]: '维度',
  [TAG_DEFINE_TYPE.METRIC]: '指标',
};
