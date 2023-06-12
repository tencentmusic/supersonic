import { MsgDataType } from 'supersonic-chat-sdk';

export enum MessageTypeEnum {
  TEXT = 'text', // 指标文本
  QUESTION = 'question',
  TAG = 'tag', // 标签
  SUGGESTION = 'suggestion', // 建议
  NO_PERMISSION = 'no_permission', // 无权限
  SEMANTIC_DETAIL = 'semantic_detail', // 语义指标/维度等信息详情
}

export type MessageItem = {
  id: string | number;
  type?: MessageTypeEnum;
  msg?: string;
  domainId?: number;
  msgData?: MsgDataType;
  quote?: string;
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
};

export enum MessageModeEnum {
  INTERPRET = 'interpret',
}
