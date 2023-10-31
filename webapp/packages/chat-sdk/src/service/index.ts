import axios from './axiosInstance';
import { ChatContextType, DrillDownDimensionType, EntityInfoType, HistoryType, MsgDataType, ParseDataType, SearchRecommendItem } from '../common/type';
import { isMobile } from '../utils/utils';

const DEFAULT_CHAT_ID = 0;

const prefix = isMobile ? '/openapi' : '/api';

export function searchRecommend(queryText: string, chatId?: number, modelId?: number, agentId?: number) {
  return axios.post<SearchRecommendItem[]>(`${prefix}/chat/query/search`, {
    queryText,
    chatId: chatId || DEFAULT_CHAT_ID,
    modelId,
    agentId
  });
}

export function chatQuery(queryText: string, chatId?: number, modelId?: number, filters?: any[]) {
  return axios.post<MsgDataType>(`${prefix}/chat/query/query`, {
    queryText,
    chatId: chatId || DEFAULT_CHAT_ID,
    modelId,
    queryFilters: filters ? {
      filters
    } : undefined,
  });
}

export function chatParse(queryText: string, chatId?: number, modelId?: number, agentId?: number, filters?: any[]) {
  return axios.post<ParseDataType>(`${prefix}/chat/query/parse`, {
    queryText,
    chatId: chatId || DEFAULT_CHAT_ID,
    modelId,
    agentId,
    queryFilters: filters ? {
      filters
    } : undefined,
  });
}

export function chatExecute(queryText: string,  chatId: number, parseInfo: ChatContextType ) {
  return axios.post<MsgDataType>(`${prefix}/chat/query/execute`, {
    queryText,
    chatId: chatId || DEFAULT_CHAT_ID,
    queryId: parseInfo.queryId,
    parseId: parseInfo.id
  });
}

export function switchEntity(entityId: string, modelId?: number, chatId?: number) {
  return axios.post<any>(`${prefix}/chat/query/switchQuery`, {
    queryText: entityId,
    modelId,
    chatId: chatId || DEFAULT_CHAT_ID,
  });
}

export function queryData(chatContext: Partial<ChatContextType>) {
  return axios.post<MsgDataType>(`${prefix}/chat/query/queryData`, chatContext);
}

export function getHistoryMsg(current: number, chatId: number = DEFAULT_CHAT_ID, pageSize: number = 10) {
  return axios.post<HistoryType>(`${prefix}/chat/manage/pageQueryInfo?chatId=${chatId}`, {
    current,
    pageSize,
  });
}

export function queryEntities(entityId: string | number, modelId: number) {
  return axios.post<any>(`${prefix}/chat/query/choice`, {
    entityId,
    modelId,
  });
}

export function updateQAFeedback(questionId: number, score: number) {
  return axios.post<any>(`${prefix}/chat/manage/updateQAFeedback?id=${questionId}&score=${score}&feedback=`);
}

export function queryDrillDownDimensions(modelId: number, metricId?: number) {
  return axios.get<{ dimensions: DrillDownDimensionType[] }>(`${prefix}/chat/recommend/metric/${modelId}${metricId ? `?metricId=${metricId}` : ''}`);
}

export function queryDimensionValues(modelId: number, bizName: string, agentId: number, elementID: number, value: string) {
  return axios.post<any>(`${prefix}/chat/query/queryDimensionValue`, { modelId, bizName, agentId, elementID, value});
}

export function querySimilarQuestions(queryText: string, agentId?: number) {
  return axios.get<any>(`${prefix}/chat/manage/getSolvedQuery?queryText=${queryText}&agentId=${agentId || 0}`);
}

export function queryEntityInfo(queryId: number, parseId: number) {
  return axios.get<EntityInfoType>(`${prefix}/chat/query/getEntityInfo?queryId=${queryId}&parseId=${parseId}`)
}
