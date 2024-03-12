import { request } from 'umi';
import { AgentType, MetricType, ModelType } from './type';

export function getAgentList() {
  return request<Result<AgentType[]>>('/api/chat/agent/getAgentList');
}

export function saveAgent(agent: AgentType) {
  return request<Result<any>>('/api/chat/agent', {
    method: agent?.id ? 'PUT' : 'POST',
    data: { ...agent, status: agent.status !== undefined ? agent.status : 1 },
  });
}

export function deleteAgent(id: number) {
  return request<Result<any>>(`/api/chat/agent/${id}`, {
    method: 'DELETE',
  });
}

export function getModelList() {
  return request<Result<ModelType[]>>('/api/chat/conf/getDomainDataSetTree', {
    method: 'GET',
  });
}

export function getMetricList(modelId: number) {
  return request<Result<{ list: MetricType[] }>>('/api/semantic/metric/queryMetric', {
    method: 'POST',
    data: {
      modelIds: [modelId],
      current: 1,
      pageSize: 2000,
    },
  });
}
