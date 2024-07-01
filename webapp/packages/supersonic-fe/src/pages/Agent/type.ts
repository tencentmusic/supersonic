export type MetricOptionType = {
  id: string;
  metricId?: number;
  modelId?: number;
};

export enum AgentToolTypeEnum {
  NL2SQL_RULE = 'NL2SQL_RULE',
  NL2SQL_LLM = 'NL2SQL_LLM',
  PLUGIN = 'PLUGIN',
}

export const AGENT_TOOL_TYPE_LIST = [
  {
    label: '规则语义解析',
    value: AgentToolTypeEnum.NL2SQL_RULE,
  },
  {
    label: '大模型语义解析',
    value: AgentToolTypeEnum.NL2SQL_LLM,
  },
  {
    label: '第三方插件',
    value: AgentToolTypeEnum.PLUGIN,
  },
];

export enum QueryModeEnum {
  METRIC = 'METRIC',
  DETAIL = 'DETAIL',
}

export const QUERY_MODE_LIST = [
  {
    label: '指标模式',
    value: QueryModeEnum.METRIC,
  },
  {
    label: '明细模式',
    value: QueryModeEnum.DETAIL,
  },
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
};

export type AgentConfigType = {
  tools: AgentToolType[];
};

export type LlmConfigType = {
  provider: string;
  baseUrl: string;
  apiKey: string;
  modelName: string;
  temperature: number;
  timeOut: number;
};

export type MultiTurnConfig = {
  enableMultiTurn: boolean;
};
export type VisualConfig = {
  defaultShowType: string;
};

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
  llmConfig?: LlmConfigType;
  multiTurnConfig?: MultiTurnConfig;
  visualConfig?: VisualConfig;
};

export type ModelType = {
  id: number | string;
  parentId: number;
  name: string;
  bizName: string;
  type: 'DOMAIN' | 'DATASET';
};

export type MetricType = {
  id: number;
  name: string;
  bizName: string;
};

export enum StatusEnum {
  PENDING = 'PENDING',
  ENABLED = 'ENABLED',
  DISABLED = 'DISABLED',
}

export enum ReviewEnum {
  POSITIVE = 'POSITIVE',
  NEGATIVE = 'NEGATIVE',
}

export type MemoryType = {
  id: number;
  question: string;
  agent_id: number;
  db_schema: string;
  s2_sql: string;
  status: StatusEnum;
  llm_review: ReviewEnum;
  llm_comment: string;
  human_review: ReviewEnum;
  human_comment: string;
  created_at: string;
  updated_at: string;
  created_by: string;
  updated_by: string;
};
