import { SemanticNodeType } from './enum';

export enum SENSITIVE_LEVEL {
  LOW = 0,
  MID = 1,
  HIGH = 2,
}

export const SENSITIVE_LEVEL_OPTIONS = [
  {
    label: '低',
    value: SENSITIVE_LEVEL.LOW,
  },
  {
    label: '中',
    value: SENSITIVE_LEVEL.MID,
  },
  {
    label: '高',
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

export const SENSITIVE_LEVEL_COLOR = {
  [SENSITIVE_LEVEL.LOW]: 'lime',
  [SENSITIVE_LEVEL.MID]: 'warning',
  [SENSITIVE_LEVEL.HIGH]: 'error',
};

export const SEMANTIC_NODE_TYPE_CONFIG = {
  [SemanticNodeType.DATASOURCE]: {
    label: '数据源',
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
