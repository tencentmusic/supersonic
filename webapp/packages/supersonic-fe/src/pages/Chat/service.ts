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

export function getDomainList() {
  return request<Result<DomainType[]>>(`${prefix}/chat/conf/domainList/view`, {
    method: 'GET',
    skipErrorHandler: true,
  });
}

export function updateQAFeedback(questionId: number, score: number) {
  return request<Result<any>>(
    `${prefix}/chat/manage/updateQAFeedback?id=${questionId}&score=${score}&feedback=`,
    {
      method: 'POST',
    },
  );
}

export function querySuggestion(domainId: number) {
  return request<Result<any>>(`${prefix}/chat/recommend/${domainId}`, {
    method: 'GET',
  });
}

export function queryRecommendQuestions() {
  return request<Result<any>>(`${prefix}/chat/recommend/question`, {
    method: 'GET',
  });
}
