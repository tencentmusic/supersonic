import request from '@/services/request';
import { DimensionType, ModelType, PluginType } from './type';

export function savePlugin(params: Partial<PluginType>) {
  const method = params.id ? 'put' : 'post';
  return request[method]<Result<any>>('/api/chat/plugin', {
    data: params,
  });
}

export function getPluginList(filters?: any) {
  return request.post<Result<any[]>>('/api/chat/plugin/query', {
    data: filters,
  });
}

export function deletePlugin(id: number) {
  return request.delete<Result<any>>(`/api/chat/plugin/${id}`);
}

export function getModelList() {
  return request.get<Result<ModelType[]>>('/api/chat/conf/getDomainDataSetTree');
}

export function getDataSetSchema(dataSetId: number) {
  return request.get<Result<{ list: DimensionType[] }>>(
    `/api/chat/conf/getDataSetSchema/${dataSetId}`,
  );
}
