import { ChatContextType, MsgDataType } from 'supersonic-chat-sdk';

export enum MessageTypeEnum {
  TEXT = 'text', // 指标文本
  QUESTION = 'question',
  TAG = 'tag', // 标签
  SUGGESTION = 'suggestion', // 建议
  NO_PERMISSION = 'no_permission', // 无权限
  SEMANTIC_DETAIL = 'semantic_detail', // 语义指标/维度等信息详情
  PLUGIN = 'PLUGIN', // 插件
  WEB_PAGE = 'WEB_PAGE', // 插件
  RECOMMEND_QUESTIONS = 'recommend_questions', // 推荐问题
  PARSE_OPTIONS = 'parse_options', // 解析选项
  AGENT_LIST = 'agent_list', // 专家列表
}

export type MessageItem = {
  id: string | number;
  type?: MessageTypeEnum;
  msg?: string;
  msgValue?: string;
  identityMsg?: string;
  modelId?: number;
  agentId?: number;
  entityId?: string;
  msgData?: MsgDataType;
  quote?: string;
  score?: number;
  feedback?: string;
  isHistory?: boolean;
  parseOptions?: ChatContextType[];
};

export type ConversationDetailType = {
  chatId: number;
  chatName: string;
  createTime?: string;
  creator?: string;
  lastQuestion?: string;
  lastTime?: string;
  initMsg?: string;
  modelId?: number;
  entityId?: string;
  agent?: AgentType;
};

export enum MessageModeEnum {
  INTERPRET = 'interpret',
}

export type ModelType = {
  id: number;
  name: string;
  bizName: string;
};

export enum PluginShowTypeEnum {
  DASHBOARD = 'DASHBOARD',
  WIDGET = 'WIDGET',
  URL = 'URL',
  TAG = 'TAG',
}

export type PluginType = {
  id: number;
  name: string;
  comment: string;
};

export type DefaultEntityType = {
  entityId: string;
  entityName: string;
  modelName?: string;
  modelId?: number;
};

export type SuggestionItemType = {
  id: number;
  model: number;
  name: string;
  bizName: string;
};

export type SuggestionType = {
  dimensions: SuggestionItemType[];
  metrics: SuggestionItemType[];
};

export type AgentType = {
  id: number;
  name: string;
  description: string;
  examples: string[];
  status: 0 | 1;
};
