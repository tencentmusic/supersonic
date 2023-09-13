import axios from './axiosInstance';
import { ChatContextType, DrillDownDimensionType, HistoryType, MsgDataType, ParseDataType, SearchRecommendItem } from '../common/type';

const DEFAULT_CHAT_ID = 12009993;

const prefix = '/api';

export function searchRecommend(queryText: string, chatId?: number, modelId?: number, agentId?: number) {
  return axios.post<Result<SearchRecommendItem[]>>(`${prefix}/chat/query/search`, {
    queryText,
    chatId: chatId || DEFAULT_CHAT_ID,
    modelId,
    agentId
  });
}

export function chatQuery(queryText: string, chatId?: number, modelId?: number, filters?: any[]) {
  return axios.post<Result<MsgDataType>>(`${prefix}/chat/query/query`, {
    queryText,
    chatId: chatId || DEFAULT_CHAT_ID,
    modelId,
    queryFilters: filters ? {
      filters
    } : undefined,
  });
}

export function chatParse(queryText: string, chatId?: number, modelId?: number, agentId?: number, filters?: any[]) {
  return axios.post<Result<ParseDataType>>(`${prefix}/chat/query/parse`, {
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
  return axios.post<Result<MsgDataType>>(`${prefix}/chat/query/execute`, {
    queryText,
    chatId: chatId || DEFAULT_CHAT_ID,
    queryId: parseInfo.queryId,
    parseId: parseInfo.id
  });
}

export function switchEntity(entityId: string, modelId?: number, chatId?: number) {
  return axios.post<Result<any>>(`${prefix}/chat/query/switchQuery`, {
    queryText: entityId,
    modelId,
    chatId: chatId || DEFAULT_CHAT_ID,
  });
}

export function queryData(chatContext: Partial<ChatContextType>) {
  return axios.post<Result<MsgDataType>>(`${prefix}/chat/query/queryData`, chatContext);
}

export function queryContext(queryText: string, chatId?: number) {
  return axios.post<Result<ChatContextType>>(`${prefix}/chat/query/queryContext`, {
    queryText,
    chatId: chatId || DEFAULT_CHAT_ID,
  });
}

export function getHistoryMsg(current: number, chatId: number = DEFAULT_CHAT_ID, pageSize: number = 10) {
  return axios.post<Result<HistoryType>>(`${prefix}/chat/manage/pageQueryInfo?chatId=${chatId}`, {
    current,
    pageSize,
  });
}

export function saveConversation(chatName: string) {
  return axios.post<Result<any>>(`${prefix}/chat/manage/save?chatName=${chatName}`);
}

export function getAllConversations() {
  return axios.get<Result<any>>(`${prefix}/chat/manage/getAll`);
}

export function queryEntities(entityId: string | number, modelId: number) {
  return axios.post<Result<any>>(`${prefix}/chat/query/choice`, {
    entityId,
    modelId,
  });
}

export function updateQAFeedback(questionId: number, score: number) {
  return axios.post<Result<any>>(`${prefix}/chat/manage/updateQAFeedback?id=${questionId}&score=${score}&feedback=`);
}

export function queryDrillDownDimensions(modelId: number) {
  return axios.get<Result<{ dimensions: DrillDownDimensionType[] }>>(`${prefix}/chat/recommend/metric/${modelId}`);
}

export function queryDimensionValues(modelId: number, bizName: string, value: string) {
  return axios.post<Result<any>>(`${prefix}/chat/query/queryDimensionValue`, { modelId, bizName, value});
}
