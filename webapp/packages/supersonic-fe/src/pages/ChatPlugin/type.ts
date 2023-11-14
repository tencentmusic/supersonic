export type PluginConfigType = {
  url: string;
  params: any;
  paramOptions: any;
  valueParams: any;
  forwardParam: any;
}

export enum PluginTypeEnum {
  WEB_PAGE = 'WEB_PAGE',
  WEB_SERVICE = 'WEB_SERVICE',
  LLM_S2SQL = 'LLM_S2SQL'
}

export enum ParseModeEnum {
  EMBEDDING_RECALL = 'EMBEDDING_RECALL',
  FUNCTION_CALL = 'FUNCTION_CALL'
}

export enum ParamTypeEnum {
  CUSTOM = 'CUSTOM',
  SEMANTIC = 'SEMANTIC',
  FORWARD = 'FORWARD'
}

export type PluginType = {
  id: number;
  type: PluginTypeEnum;
  modelList: number[];
  pattern: string;
  parseMode: ParseModeEnum;
  parseModeConfig: string;
  name: string;
  config: PluginConfigType;
}

export type ModelType = {
  id: number | string;
  parentId: number;
  name: string;
  bizName: string;
};

export type DimensionType = {
  id: number;
  name: string;
  bizName: string;
};

export type FunctionParamType = {
  type: string;
  properties: Record<string, { type: string, description: string }>;
  required: string[];
}

export type FunctionType = {
  name: string;
  description: string;
  parameters: FunctionParamType;
  examples: string[];
}

export type FunctionParamFormItemType = {
  id: string;
  name?: string;
  type?: string;
  description?: string;
}
