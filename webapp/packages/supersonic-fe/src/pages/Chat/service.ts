import { request } from 'umi';
import { ModelType } from './type';

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

export function getMiniProgramList(entityId: string, modelId: number) {
  return request<Result<any>>(
    `${prefix}/chat/plugin/extend/getAvailablePlugin/${entityId}/${modelId}`,
    {
      method: 'GET',
      skipErrorHandler: true,
    },
  );
}

export function getModelList() {
  return request<Result<ModelType[]>>(`${prefix}/chat/conf/modelList/view`, {
    method: 'GET',
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

export function queryMetricSuggestion(modelId: number) {
  return request<Result<any>>(`${prefix}/chat/recommend/metric/${modelId}`, {
    method: 'GET',
  });
}

export function querySuggestion(modelId: number) {
  return request<Result<any>>(`${prefix}/chat/recommend/${modelId}`, {
    method: 'GET',
  });
}

export function queryRecommendQuestions() {
  return request<Result<any>>(`${prefix}/chat/recommend/question`, {
    method: 'GET',
  });
}
