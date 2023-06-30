import { request } from 'umi';
import { DomainType } from './type';

const prefix = '/api';

export function saveConversation(chatName: string) {
  return request<Result<any>>(`${prefix}/chat/manage/save?chatName=${chatName}`, {
    method: 'POST',
  });
}

export function updateConversationName(chatName: string, chatId: number = 0) {
  return request<Result<any>>(
    `${prefix}/chat/manage/updateChatName?chatName=${chatName}&chatId=${chatId}`,
    { method: 'POST' },
  );
}

export function deleteConversation(chatId: number) {
  return request<Result<any>>(`${prefix}/chat/manage/delete?chatId=${chatId}`, { method: 'POST' });
}

export function getAllConversations() {
  return request<Result<any>>(`${prefix}/chat/manage/getAll`);
}

export function getMiniProgramList(id: string, type: string) {
  return request<Result<any>>(`/openapi/bd-bi/api/polaris/sql/getInterpretList/${id}/${type}`, {
    method: 'GET',
    skipErrorHandler: true,
  });
}

export function getDomainList() {
  return request<Result<DomainType[]>>(`${prefix}/semantic/domain/getDomainList`, {
    method: 'GET',
    skipErrorHandler: true,
  });
}
