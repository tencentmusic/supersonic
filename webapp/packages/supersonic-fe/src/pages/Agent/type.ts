export type MetricOptionType = {
  id: string;
  metricId?: number;
  modelId?: number;
};

export enum AgentToolTypeEnum {
  NL2SQL_RULE = 'NL2SQL_RULE',
  NL2SQL_LLM = 'NL2SQL_LLM',
  PLUGIN = 'PLUGIN',
  DATASET = 'DATASET',
}

export enum QueryModeEnum {
  METRIC = 'METRIC',
  DETAIL = 'DETAIL',
}

export const QUERY_MODE_LIST = [
  {
    label: '聚合模式',
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

// export type EmbeddingModelConfigType = {
//   apiKey: string;
//   baseUrl: string;
//   // logRequests: true,
//   // logResponses: true,
//   // maxRetries: number,
//   // maxToken: number,
//   modelName: string;
//   modelPath: string;
//   provider: string;
//   vocabularyPath: string;
// };

export type MultiTurnConfig = {
  enableMultiTurn: boolean;
};
export type VisualConfig = {
  defaultShowType: string;
};

export type ChatAppConfigItem = {
  key: string;
  name: string;
  description: string;
  prompt: string;
  enable: boolean;
  chatModelId: number;
};

export type ChatAppConfig = Record<string, ChatAppConfigItem>;

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
  enableFeedback?: 0 | 1;
  toolConfig?: string;
  chatAppConfig: ChatAppConfig;
  multiTurnConfig?: MultiTurnConfig;
  visualConfig?: VisualConfig;
  admins?: string[];
  adminOrgs?: string[];
  viewers?: string[];
  viewOrgs?: string[];
  isOpen: number;
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
