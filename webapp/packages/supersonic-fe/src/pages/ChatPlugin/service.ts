import { request } from "umi";
import { DimensionType, DomainType, PluginType } from "./type";

export function savePlugin(params: Partial<PluginType>) {
  return request<Result<any>>('/api/chat/plugin', {
    method: params.id ? 'PUT' : 'POST',
    data: params,
  });
}

export function getPluginList(filters?: any) {
  return request<Result<any[]>>('/api/chat/plugin/query', {
    method: 'POST',
    data: filters
  });
}

export function deletePlugin(id: number) {
  return request<Result<any>>(`/api/chat/plugin/${id}`, {
    method: 'DELETE',
  });
}

export function getDomainList() {
  return request<Result<DomainType[]>>('/api/chat/conf/domainList', {
    method: 'GET',
  });
}

export function getDimensionList(domainId: number) {
  return request<Result<{list: DimensionType[]}>>('/api/semantic/dimension/queryDimension', {
    method: 'POST',
    data: {
      domainIds: [domainId],
      current: 1,
      pageSize: 2000
    }
  });
}
