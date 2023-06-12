import request from 'umi-request';

export function getDomainList(): Promise<any> {
  if (window.RUNNING_ENV === 'chat') {
    return request.get(`${process.env.CHAT_API_BASE_URL}conf/domainList`);
  }
  return request.get(`${process.env.API_BASE_URL}domain/getDomainList`);
}

export function getDatasourceList(data: any): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}datasource/getDatasourceList/${data.domainId}`);
}

export function getDomainDetail(data: any): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}domain/getDomain/${data.domainId}`);
}

export function createDomain(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}domain/createDomain`, {
    data,
  });
}

export function updateDomain(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}domain/updateDomain`, {
    data,
  });
}

export function createDatasource(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}datasource/createDatasource`, {
    data,
  });
}

export function updateDatasource(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}datasource/updateDatasource`, {
    data,
  });
}

export function getDimensionList(data: any): Promise<any> {
  const queryParams = {
    data: { current: 1, pageSize: 999999, ...data },
  };
  if (window.RUNNING_ENV === 'chat') {
    return request.post(`${process.env.CHAT_API_BASE_URL}conf/dimension/page`, queryParams);
  }
  return request.post(`${process.env.API_BASE_URL}dimension/queryDimension`, queryParams);
}

export function createDimension(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/createDimension`, {
    data,
  });
}

export function updateDimension(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/updateDimension`, {
    data,
  });
}

export function queryMetric(data: any): Promise<any> {
  const queryParams = {
    data: { current: 1, pageSize: 999999, ...data },
  };
  if (window.RUNNING_ENV === 'chat') {
    return request.post(`${process.env.CHAT_API_BASE_URL}conf/metric/page`, queryParams);
  }
  return request.post(`${process.env.API_BASE_URL}metric/queryMetric`, queryParams);
}

export function creatExprMetric(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/creatExprMetric`, {
    data,
  });
}

export function updateExprMetric(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/updateExprMetric`, {
    data,
  });
}

export function getMeasureListByDomainId(domainId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}datasource/getMeasureListOfDomain/${domainId}`);
}

export function deleteDatasource(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}datasource/deleteDatasource/${id}`, {
    method: 'DELETE',
  });
}

export function deleteDimension(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}dimension/deleteDimension/${id}`, {
    method: 'DELETE',
  });
}

export function deleteMetric(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}metric/deleteMetric/${id}`, {
    method: 'DELETE',
  });
}

export function deleteDomain(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}domain/deleteDomain/${id}`, {
    method: 'DELETE',
  });
}

export function getGroupAuthInfo(id: string): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}queryGroup`, {
    method: 'GET',
    params: {
      domainId: id,
    },
  });
}

export function createGroupAuth(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}createGroup`, {
    method: 'POST',
    data,
  });
}

export function updateGroupAuth(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}updateGroup`, {
    method: 'POST',
    data,
  });
}

export function removeGroupAuth(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}removeGroup`, {
    method: 'POST',
    data,
  });
}

export function addDomainExtend(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}conf`, {
    method: 'POST',
    data,
  });
}

export function editDomainExtend(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}conf`, {
    method: 'PUT',
    data,
  });
}

export function getDomainExtendConfig(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}conf/search`, {
    method: 'POST',
    data,
  });
}

export function getDomainExtendDetailConfig(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}conf/richDesc/${data.domainId}`, {
    method: 'GET',
  });
}

export function getDatasourceRelaList(id?: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}datasource/getDatasourceRelaList/${id}`, {
    method: 'GET',
  });
}

export function createOrUpdateDatasourceRela(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/createOrUpdateDatasourceRela`, {
    method: 'POST',
    data,
  });
}

export function createOrUpdateViewInfo(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/createOrUpdateViewInfo`, {
    method: 'POST',
    data,
  });
}

export function getViewInfoList(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/getViewInfoList/${domainId}`, {
    method: 'GET',
  });
}

export function deleteDatasourceRela(domainId: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/deleteDatasourceRela/${domainId}`, {
    method: 'DELETE',
  });
}

export function getDatabaseByDomainId(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getDatabaseByDomainId/${domainId}`, {
    method: 'GET',
  });
}

export function getDomainSchemaRela(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/getDomainSchemaRela/${domainId}`, {
    method: 'GET',
  });
}

export type SaveDatabaseParams = {
  domainId: number;
  name: string;
  type: string;
  host: string;
  port: string;
  username: string;
  password: string;
  database?: string;
  description?: string;
};

export function saveDatabase(data: SaveDatabaseParams): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/createOrUpdateDatabase`, {
    method: 'POST',
    data,
  });
}

export function testDatabaseConnect(data: SaveDatabaseParams): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/testConnect`, {
    method: 'POST',
    data,
  });
}
