import { ChatContextType, MsgDataType, ParseTimeCostType, SendMsgParamsType } from "../common/type";

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
  parseInfos?: ChatContextType[];
  parseTimeCost?: ParseTimeCostType;
  msgData?: MsgDataType;
  quote?: string;
  score?: number;
  feedback?: string;
  filters?: any;
};

export type ConversationDetailType = {
  chatId: number;
  chatName: string;
  createTime?: string;
  creator?: string;
  lastQuestion?: string;
  lastTime?: string;
  initialMsgParams?: SendMsgParamsType;
  isAdd?: boolean;
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
  initialSendMsgParams?: SendMsgParamsType;
};
