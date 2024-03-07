import { request } from 'umi';
import { DimensionType, ModelType, PluginType } from './type';

export function savePlugin(params: Partial<PluginType>) {
  return request<Result<any>>('/api/chat/plugin', {
    method: params.id ? 'PUT' : 'POST',
    data: params,
  });
}

export function getPluginList(filters?: any) {
  return request<Result<any[]>>('/api/chat/plugin/query', {
    method: 'POST',
    data: filters,
  });
}

export function deletePlugin(id: number) {
  return request<Result<any>>(`/api/chat/plugin/${id}`, {
    method: 'DELETE',
  });
}

export function getModelList() {
  return request<Result<ModelType[]>>('/api/chat/conf/getDomainDataSetTree', {
    method: 'GET',
  });
}

export function getDimensionList(modelId: number) {
  return request<Result<{ list: DimensionType[] }>>('/api/semantic/dimension/queryDimension', {
    method: 'POST',
    data: {
      modelIds: [modelId],
      current: 1,
      pageSize: 2000,
    },
  });
}
