import request from 'umi-request';
import moment from 'moment';
import { DateRangeType } from '@/components/MDatePicker/type';
import { IDataSource } from './data';

const getRunningEnv = () => {
  return window.location.pathname.includes('/chatSetting/') ? 'chat' : 'semantic';
};

export function getDomainList(): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}domain/getDomainList`);
}

export function getDomainDetail(data: any): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}domain/getDomain/${data.modelId}`);
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
  const { domainId, modelId } = data;
  const queryParams = {
    data: {
      current: 1,
      pageSize: 999999,
      ...data,
      ...(domainId ? { domainIds: [domainId] } : {}),
      ...(modelId ? { modelIds: [modelId] } : {}),
    },
  };
  return request.post(`${process.env.API_BASE_URL}dimension/queryDimension`, queryParams);
}

export function saveCommonDimension(data: any): Promise<any> {
  if (data.id) {
    return request(`${process.env.API_BASE_URL}commonDimension`, {
      method: 'PUT',
      data,
    });
  }
  return request.post(`${process.env.API_BASE_URL}commonDimension`, {
    data,
  });
}

export function deleteCommonDimension(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}commonDimension/${id}`, {
    method: 'DELETE',
  });
}

export function getDimensionInModelCluster(modelId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}dimension/getDimensionInModelCluster/${modelId}`);
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

export function updateDimensionAliasValue(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/updateDimension/alias/value`, {
    data,
  });
}

export function mockDimensionAlias(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/mockDimensionAlias`, {
    data,
  });
}

export function mockDimensionValuesAlias(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/mockDimensionValuesAlias`, {
    data,
  });
}

export function getDictData(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}knowledge/dict/data`, {
    data,
  });
}

export function queryMetric(data: any): Promise<any> {
  const { domainId, modelId } = data;
  const queryParams = {
    data: {
      current: 1,
      pageSize: 999999,
      ...data,
      ...(domainId ? { domainIds: [domainId] } : {}),
      ...(modelId ? { modelIds: [modelId] } : {}),
    },
  };
  return request.post(`${process.env.API_BASE_URL}metric/queryMetric`, queryParams);
}

export function createMetric(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/createMetric`, {
    data,
  });
}

export function updateMetric(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/updateMetric`, {
    data,
  });
}

export function batchUpdateMetricStatus(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/batchUpdateStatus`, {
    data,
  });
}

export function batchUpdateDimensionStatus(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/batchUpdateStatus`, {
    data,
  });
}

export async function batchDownloadMetric(data: any): Promise<any> {
  const response = await request.post(`${process.env.API_BASE_URL}query/downloadBatch/metric`, {
    responseType: 'blob',
    getResponse: true,
    data,
  });

  downloadStruct(response.data);
}

export function mockMetricAlias(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/mockMetricAlias`, {
    data,
  });
}

export function getMetricTags(): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}metric/getMetricTags`);
}

export function getMetricData(metricId: string | number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}metric/getMetric/${metricId}`);
}

export function getDrillDownDimension(metricId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}metric/getDrillDownDimension`, {
    params: { metricId },
  });
}

export function getMeasureListByModelId(modelId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}datasource/getMeasureListOfModel/${modelId}`);
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

export function getGroupAuthInfo(modelId: number): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}queryGroup`, {
    method: 'GET',
    params: { modelId },
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

export function createOrUpdateModelRela(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}modelRela`, {
    method: data?.id ? 'PUT' : 'POST',
    data,
  });
}

export function deleteModelRela(id: any): Promise<any> {
  if (!id) {
    return;
  }
  return request(`${process.env.API_BASE_URL}modelRela/${id}`, {
    method: 'DELETE',
  });
}

export function getModelRelaList(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}modelRela/list`, {
    method: 'GET',
    params: { domainId },
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

export function deleteViewInfo(recordId: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/deleteViewInfo/${recordId}`, {
    method: 'DELETE',
  });
}

export function deleteDatasourceRela(domainId: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/deleteDatasourceRela/${domainId}`, {
    method: 'DELETE',
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

export function getDatabaseList(): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getDatabaseList`, {
    method: 'GET',
  });
}

export function deleteDatabase(domainId: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/${domainId}`, {
    method: 'DELETE',
  });
}

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

type ExcuteSqlParams = {
  sql: string;
  id: number;
  sqlVariables: IDataSource.ISqlParamsItem[];
};

// 执行脚本
export async function executeSql(params: ExcuteSqlParams) {
  const data = { ...params };
  return request.post(`${process.env.API_BASE_URL}database/executeSql`, { data });
}

export async function listColumnsBySql(data: { databaseId: number; sql: string }) {
  return request.post(`${process.env.API_BASE_URL}database/listColumnsBySql`, {
    data,
  });
}

export function getCatalogs(dbId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getCatalogs`, {
    method: 'GET',
    params: {
      id: dbId,
    },
  });
}

export function getDbNames(dbId: number, catalog: string): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getDbNames`, {
    method: 'GET',
    params: {
      id: dbId,
      catalog: catalog,
    },
  });
}

export function getTables(databaseId: number, catalog: string, dbName: string): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getTables`, {
    method: 'GET',
    params: {
      databaseId,
      catalog: catalog,
      db: dbName,
    },
  });
}

export function getColumns(databaseId: number, catalog: string, dbName: string, tableName: string): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getColumnsByName`, {
    method: 'GET',
    params: {
      databaseId,
      catalog: catalog,
      db: dbName,
      table: tableName,
    },
  });
}

export function getModelList(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/getModelList/${domainId}`, {
    method: 'GET',
  });
}

export function createModel(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/createModel`, {
    method: 'POST',
    data,
  });
}
export function updateModel(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/updateModel`, {
    method: 'POST',
    data,
  });
}

export function batchUpdateModelStatus(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/batchUpdateStatus`, {
    method: 'POST',
    data,
  });
}

export function deleteModel(modelId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/deleteModel/${modelId}`, {
    method: 'DELETE',
  });
}

export function getUnAvailableItem(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/getUnAvailableItem`, {
    method: 'POST',
    data,
  });
}

export function getModelDetail(data: any): Promise<any> {
  if (!data.modelId) {
    return {};
  }
  return request.get(`${process.env.API_BASE_URL}model/getModel/${data.modelId}`);
}

export function getMetricsToCreateNewMetric(data: any): Promise<any> {
  return request.get(
    `${process.env.API_BASE_URL}metric/getMetricsToCreateNewMetric/${data.modelId}`,
  );
}

export function getAllModelByDomainId(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/getAllModelByDomainId`, {
    method: 'GET',
    params: {
      domainId,
    },
  });
}

export function createDictTask(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}knowledge/task`, {
    method: 'POST',
    data,
  });
}

export function createDictConfig(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}knowledge/conf`, {
    method: 'POST',
    data,
  });
}

export function editDictConfig(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}knowledge/conf`, {
    method: 'PUT',
    data,
  });
}

export function deleteDictTask(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}knowledge/task/delete`, {
    method: 'PUT',
    data,
  });
}

export function searchDictLatestTaskList(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}knowledge/task/search`, {
    method: 'POST',
    data,
  });
}

export function searchKnowledgeConfigQuery(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}knowledge/conf/query`, {
    method: 'POST',
    data,
  });
}

const downloadStruct = (blob: Blob) => {
  const fieldName = `supersonic_${moment().format('YYYYMMDDhhmmss')}.xlsx`;
  const link = document.createElement('a');
  link.href = URL.createObjectURL(new Blob([blob]));
  link.download = fieldName;
  document.body.appendChild(link);
  link.click();
  URL.revokeObjectURL(link.href);
  document.body.removeChild(link);
};

export function queryDimValue(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}dimension/queryDimValue`, {
    method: 'POST',
    data,
  });
}

export async function queryStruct({
  domainId,
  dateField = 'sys_imp_date',
  startDate,
  endDate,
  download = false,
  period = DateRangeType.DAY,
  dimensionIds = [],
  metricIds,
  filters = [],
  isTransform,
}: {
  domainId: number;
  metricIds: number[];
  dateField: string;
  startDate: string;
  endDate: string;
  download?: boolean;
  dimensionIds: number[];
  period: DateRangeType;
  filters?: string[];
  isTransform: boolean;
}): Promise<any> {
  const response = await request(
    `${process.env.API_BASE_URL}query/${download ? 'download/' : ''}metric`,
    {
      method: 'POST',
      ...(download ? { responseType: 'blob', getResponse: true } : {}),
      data: {
        domainId,
        filters,
        isTransform,
        metricIds,
        dimensionIds,
        orders: [{ column: dateField, direction: 'desc' }],
        params: [],
        dateInfo: {
          dateMode: 'BETWEEN',
          startDate,
          endDate,
          dateList: [],
          unit: 7,
          groupByDate: true,
          period,
          text: 'null',
        },
        limit: 2000,
        nativeQuery: false,
      },
    },
  );
  if (download) {
    downloadStruct(response.data);
  } else {
    return response;
  }
}

export function indicatorStarState(data: {
  id: number;
  type: string;
  state: boolean;
}): Promise<any> {
  const { id, state, type } = data;
  if (state) {
    return request(`${process.env.API_BASE_URL}collect/createCollectionIndicators`, {
      method: 'POST',
      data: { collectId: id, type },
    });
  } else {
    // return request(`${process.env.API_BASE_URL}collect/deleteCollectionIndicators/${id}`, {
    //   method: 'DELETE',
    // });
    return request(`${process.env.API_BASE_URL}collect/deleteCollectionIndicators`, {
      method: 'POST',
      data: { collectId: id, type },
    });
  }
}

export function getDatabaseParameters(): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}database/getDatabaseParameters`);
}

export function getDatabaseDetail(id: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}database/${id}`);
}

export function getDataSetList(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}dataSet/getDataSetList`, {
    method: 'GET',
    params: { domainId },
  });
}

export function getDataSetDetail(id: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}dataSet/${id}`, {
    method: 'GET',
  });
}

export function createView(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}dataSet`, {
    method: 'POST',
    data,
  });
}
export function updateView(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}dataSet`, {
    method: 'PUT',
    data,
  });
}

export function deleteView(viewId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}dataSet/${viewId}`, {
    method: 'DELETE',
  });
}

export function getTagList(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tag/queryTag/market`, {
    method: 'POST',
    data: { pageSize: 9999, ...data },
  });
}

export function deleteTag(tagId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}tag/delete/${tagId}`, {
    method: 'DELETE',
  });
}

export function batchUpdateTagStatus(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}tag/batchUpdateStatus`, {
    data,
  });
}

export function createTag(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tag/create`, {
    method: 'POST',
    data,
  });
}

export function updateTag(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tag/update`, {
    method: 'POST',
    data,
  });
}

export function getTagData(tagId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}tag/getTag/${tagId}`, {
    method: 'GET',
  });
}

export function getTagValueDistribution(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tag/value/distribution`, {
    method: 'POST',
    data,
  });
}

export function batchCreateTag(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tag/create/batch`, {
    method: 'POST',
    data,
  });
}

export function batchDeleteTag(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tag/delete/batch`, {
    method: 'POST',
    data,
  });
}

export function batchMetricPublish(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}metric/batchPublish`, {
    method: 'POST',
    data,
  });
}

export function batchMetricUnPublish(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}metric/batchUnPublish`, {
    method: 'POST',
    data,
  });
}

export function createTagObject(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tagObject/create`, {
    method: 'POST',
    data,
  });
}

export function updateTagObject(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tagObject/update`, {
    method: 'POST',
    data,
  });
}

export function deleteTagObject(id: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}tagObject/delete/${id}`, {
    method: 'DELETE',
  });
}

export function getTagObjectList(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}tagObject/query`, {
    method: 'POST',
    data: { pageSize: 9999, status: 1, ...data },
  });
}

export function getMetricClassifications(): Promise<any> {
  return request(`${process.env.API_BASE_URL}metric/getMetricClassifications`, {
    method: 'GET',
  });
}

export function batchUpdateClassifications(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}metric/batchUpdateClassifications`, {
    method: 'POST',
    data: { ...data },
  });
}

export function batchUpdateDimensionSensitiveLevel(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}dimension/batchUpdateSensitiveLevel`, {
    method: 'POST',
    data: { ...data },
  });
}

export function batchUpdateMetricSensitiveLevel(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}metric/batchUpdateSensitiveLevel`, {
    method: 'POST',
    data: { ...data },
  });
}

export function getTermList(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}term`, {
    method: 'GET',
    params: data,
  });
}

export function saveOrUpdate(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}term/saveOrUpdate`, {
    method: 'POST',
    data: { ...data },
  });
}

export function deleteTerm(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}term/deleteBatch`, {
    method: 'POST',
    data: { ...data },
  });
}

export function createLlmConfig(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}chat/model`, {
    method: 'POST',
    data: { ...data },
  });
}

export function saveLlmConfig(data: any): Promise<any> {
  if (data.id) {
    return request(`${process.env.CHAT_API_BASE_URL}model`, {
      method: 'PUT',
      data,
    });
  }
  return request(`${process.env.CHAT_API_BASE_URL}model`, {
    method: 'POST',
    data,
  });
}

export function deleteLlmConfig(id: number): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}model/${id}`, {
    method: 'DELETE',
  });
}
