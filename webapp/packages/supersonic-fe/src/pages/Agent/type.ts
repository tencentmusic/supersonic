export type MetricOptionType = {
  id: string;
  metricId?: number;
  modelId?: number;
}

export enum AgentToolTypeEnum {
  RULE = 'RULE',
  DSL = 'DSL',
  PLUGIN = 'PLUGIN',
  INTERPRET = 'INTERPRET'
}

export enum QueryModeEnum {
  ENTITY_DETAIL = 'ENTITY_DETAIL',
  ENTITY_LIST_FILTER = 'ENTITY_LIST_FILTER',
  ENTITY_ID = 'ENTITY_ID',
  METRIC_ENTITY = 'METRIC_ENTITY',
  METRIC_FILTER = 'METRIC_FILTER',
  METRIC_GROUPBY = 'METRIC_GROUPBY',
  METRIC_MODEL = 'METRIC_MODEL',
  METRIC_ORDERBY = 'METRIC_ORDERBY'
}

export const AGENT_TOOL_TYPE_LIST = [
  {
    label: '规则语义解析',
    value: AgentToolTypeEnum.RULE
  },
  {
    label: '大模型语义解析',
    value: AgentToolTypeEnum.DSL
  },
  {
    label: '大模型指标解读',
    value: AgentToolTypeEnum.INTERPRET
  },
  {
    label: '第三方插件',
    value: AgentToolTypeEnum.PLUGIN
  },
]

export const QUERY_MODE_LIST = [
  {
    label: '实体明细(查询维度信息)',
    value: QueryModeEnum.ENTITY_DETAIL
  },
  {
    label: '实体圈选',
    value: QueryModeEnum.ENTITY_LIST_FILTER
  },
  {
    label: '实体查询(按ID查询)',
    value: QueryModeEnum.ENTITY_ID
  },
  {
    label: '指标查询(带实体)',
    value: QueryModeEnum.METRIC_ENTITY
  },
  {
    label: '指标查询(带条件)',
    value: QueryModeEnum.METRIC_FILTER
  },
  {
    label: '指标查询(按维度分组)',
    value: QueryModeEnum.METRIC_GROUPBY
  },
  {
    label: '指标查询(不带条件)',
    value: QueryModeEnum.METRIC_MODEL
  },
  {
    label: '按指标排序',
    value: QueryModeEnum.METRIC_ORDERBY
  }
];

export type AgentToolType = {
  id?: string;
  type: AgentToolTypeEnum;
  name: string;
  queryModes?: QueryModeEnum[];
  plugins?: number[];
  metricOptions?: MetricOptionType[];
  exampleQuestions?: string[];
  modelIds?: number[];
}

export type AgentConfigType = {
  tools: AgentToolType[];
}

export type AgentType = {
  id?: number;
  name?: string;
  description?: string;
  createdBy?: string;
  updatedBy?: string;
  createdAt?: string;
  updatedAt?: string;
  examples?: string[];
  status?: 0 | 1;
  enableSearch?: 0 | 1;
  agentConfig?: AgentConfigType;
}

export type ModelType = {
  id: number | string;
  parentId: number;
  name: string;
  bizName: string;
};

export type MetricType = {
  id: number;
  name: string;
  bizName: string;
};
