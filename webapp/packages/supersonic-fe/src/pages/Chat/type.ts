import { MsgDataType } from 'supersonic-chat-sdk';

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
}

export type MessageItem = {
  id: string | number;
  type?: MessageTypeEnum;
  msg?: string;
  msgValue?: string;
  identityMsg?: string;
  domainId?: number;
  entityId?: string;
  msgData?: MsgDataType;
  quote?: string;
  score?: number;
  feedback?: string;
  isHistory?: boolean;
};

export type ConversationDetailType = {
  chatId: number;
  chatName: string;
  createTime?: string;
  creator?: string;
  lastQuestion?: string;
  lastTime?: string;
  initMsg?: string;
  domainId?: number;
  entityId?: string;
};

export enum MessageModeEnum {
  INTERPRET = 'interpret',
}

export type DomainType = {
  id: number;
  parentId: number;
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
  domainName?: string;
};

export type SuggestionItemType = {
  id: number;
  domain: number;
  name: string;
  bizName: string;
};

export type SuggestionType = {
  dimensions: SuggestionItemType[];
  metrics: SuggestionItemType[];
};
