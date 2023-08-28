import { SemanticNodeType } from './enum';

export const SENSITIVE_LEVEL_OPTIONS = [
  {
    label: '低',
    value: 0,
  },
  {
    label: '中',
    value: 1,
  },
  {
    label: '高',
    value: 2,
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
