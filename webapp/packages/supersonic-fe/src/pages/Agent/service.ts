import request from '@/services/request';
import { StatusEnum } from '@/common/constants';
import { AgentType, MemoryType, MetricType, ModelType } from './type';

export function getAgentList() {
  return request.get<Result<AgentType[]>>('/api/chat/agent/getAgentList');
}

export function saveAgent(agent: AgentType) {
  const method = agent?.id ? 'put' : 'post';
  return request[method]<Result<any>>('/api/chat/agent', {
    data: { ...agent, status: agent.status !== undefined ? agent.status : StatusEnum.ENABLED },
  });
}

export function deleteAgent(id: number) {
  return request.delete<Result<any>>(`/api/chat/agent/${id}`);
}

export function getModelList() {
  return request.get<Result<ModelType[]>>('/api/chat/conf/getDomainDataSetTree');
}

export function getMetricList(modelId: number) {
  return request.post<Result<{ list: MetricType[] }>>('/api/semantic/metric/queryMetric', {
    data: {
      modelIds: [modelId],
      current: 1,
      pageSize: 2000,
    },
  });
}

export function getMemeoryList(data: {
  agentId: number;
  chatMemoryFilter: any;
  current: number;
  pageSize: number;
}) {
  const { agentId, chatMemoryFilter, current, pageSize } = data;
  return request.post<Result<{ list: MemoryType[]; total: number; pageNum: number }>>('/api/chat/memory/pageMemories', {
    data: {
      ...data,
      chatMemoryFilter: { agentId, ...chatMemoryFilter },
      current,
      pageSize: pageSize || 10,
      sort: 'desc',
    },
  });
}

export function saveMemory(data: MemoryType) {
  return request.post<Result<string>>('/api/chat/memory/updateMemory', {
    data,
  });
}

export function batchDeleteMemory(ids: number[]) {
  return request.post<Result<string>>('/api/chat/memory/batchDelete', {
    data: { ids },
  });
}

export function getToolTypes(): Promise<any> {
  return request.get(`${process.env.CHAT_API_BASE_URL}agent/getToolTypes`);
}

export function createMemory(data: any) {
  return request.post<Result<string>>('/api/chat/memory/createMemory', {
    data,
  });
}
