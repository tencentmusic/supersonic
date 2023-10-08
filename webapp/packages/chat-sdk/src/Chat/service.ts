import axios from '../service/axiosInstance';
import { isMobile } from '../utils/utils';
import { AgentType, ModelType } from './type';

const prefix = isMobile ? '/openapi' : '/api';

export function saveConversation(chatName: string, agentId: number) {
  return axios.post<any>(
    `${prefix}/chat/manage/save?chatName=${chatName}&agentId=${agentId}`
  );
}

export function updateConversationName(chatName: string, chatId: number = 0) {
  return axios.post<any>(
    `${prefix}/chat/manage/updateChatName?chatName=${chatName}&chatId=${chatId}`,
  );
}

export function deleteConversation(chatId: number) {
  return axios.post<any>(`${prefix}/chat/manage/delete?chatId=${chatId}`);
}

export function getAllConversations(agentId?: number) {
  return axios.get<any>(`${prefix}/chat/manage/getAll`, { params: { agentId } });
}

export function getModelList() {
  return axios.get<ModelType[]>(`${prefix}/chat/conf/modelList/view`);
}

export function updateQAFeedback(questionId: number, score: number) {
  return axios.post<any>(
    `${prefix}/chat/manage/updateQAFeedback?id=${questionId}&score=${score}&feedback=`,
  );
}

export function queryMetricSuggestion(modelId: number) {
  return axios.get<any>(`${prefix}/chat/recommend/metric/${modelId}`);
}

export function querySuggestion(modelId: number) {
  return axios.get<any>(`${prefix}/chat/recommend/${modelId}`);
}

export function queryRecommendQuestions() {
  return axios.get<any>(`${prefix}/chat/recommend/question`);
}

export function queryAgentList() {
  return axios.get<AgentType[]>(`${prefix}/chat/agent/getAgentList`);
}
