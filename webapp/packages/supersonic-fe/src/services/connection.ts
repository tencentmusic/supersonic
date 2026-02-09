import { request } from 'umi';

export interface ConnectionDO {
  id?: number;
  name: string;
  description?: string;
  sourceDatabaseId: number;
  destinationDatabaseId: number;
  status?: string;
  statusUpdatedAt?: string;
  statusMessage?: string;
  configuredCatalog?: string;
  discoveredCatalog?: string;
  discoveredCatalogAt?: string;
  schemaChangeStatus?: string;
  schemaChangeDetail?: string;
  scheduleType?: string;
  cronExpression?: string;
  scheduleUnits?: number;
  scheduleTimeUnit?: string;
  state?: string;
  stateType?: string;
  retryCount?: number;
  advancedConfig?: string;
  quartzJobKey?: string;
  createdBy?: string;
  updatedBy?: string;
  tenantId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConnectionEventDO {
  id: number;
  connectionId: number;
  eventType: string;
  eventTime: string;
  eventData?: string;
  userId?: number;
  userName?: string;
  jobId?: number;
  tenantId: number;
}

export interface DataSyncExecutionDO {
  id: number;
  syncConfigId?: number;
  connectionId?: number;
  jobType?: string;
  attemptNumber?: number;
  status: string;
  startTime?: string;
  endTime?: string;
  rowsRead?: number;
  rowsWritten?: number;
  bytesSynced?: number;
  watermarkValue?: string;
  errorMessage?: string;
  tenantId: number;
}

export interface DiscoveredSchema {
  tables: DiscoveredTable[];
}

export interface DiscoveredTable {
  tableName: string;
  tableComment?: string;
  columns: DiscoveredColumn[];
}

export interface DiscoveredColumn {
  columnName: string;
  columnType: string;
  columnComment?: string;
  nullable: boolean;
}

export interface SchemaChange {
  status: string;
  changes: StreamChange[];
}

export interface StreamChange {
  streamName: string;
  changeType: string;
  columnChanges?: ColumnChange[];
}

export interface ColumnChange {
  columnName: string;
  changeType: string;
  previousType?: string;
  currentType?: string;
}

export interface ConfiguredCatalog {
  streams: ConfiguredStream[];
}

export interface ConfiguredStream {
  streamName: string;
  namespace?: string;
  syncMode?: string;
  destinationSyncMode?: string;
  cursorField?: string;
  primaryKey?: string[];
  selected?: boolean;
  destinationTable?: string;
  columns?: string;
  batchSize?: number;
  preSql?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

const API_PREFIX = '/api/v1/connections';

export async function createConnection(connection: ConnectionDO) {
  return request<ConnectionDO>(API_PREFIX, {
    method: 'POST',
    data: connection,
  });
}

export async function updateConnection(id: number, connection: Partial<ConnectionDO>) {
  return request<ConnectionDO>(`${API_PREFIX}/${id}`, {
    method: 'PATCH',
    data: connection,
  });
}

export async function deleteConnection(id: number) {
  return request<void>(`${API_PREFIX}/${id}`, {
    method: 'DELETE',
  });
}

export async function getConnectionById(id: number) {
  return request<ConnectionDO>(`${API_PREFIX}/${id}`, {
    method: 'GET',
  });
}

export async function listConnections(params: {
  current?: number;
  pageSize?: number;
  sourceDbId?: number;
  destDbId?: number;
  status?: string;
}) {
  return request<PageResult<ConnectionDO>>(API_PREFIX, {
    method: 'GET',
    params: {
      current: params.current || 1,
      pageSize: params.pageSize || 20,
      sourceDbId: params.sourceDbId,
      destDbId: params.destDbId,
      status: params.status,
    },
  });
}

export async function pauseConnection(id: number) {
  return request<void>(`${API_PREFIX}/${id}:pause`, {
    method: 'POST',
  });
}

export async function resumeConnection(id: number) {
  return request<void>(`${API_PREFIX}/${id}:resume`, {
    method: 'POST',
  });
}

export async function deprecateConnection(id: number, reason?: string) {
  return request<void>(`${API_PREFIX}/${id}:deprecate`, {
    method: 'POST',
    data: reason ? { reason } : {},
  });
}

export async function triggerSync(id: number) {
  return request<void>(`${API_PREFIX}/${id}:sync`, {
    method: 'POST',
  });
}

export async function resetState(id: number, streams?: string[]) {
  return request<void>(`${API_PREFIX}/${id}:resetState`, {
    method: 'POST',
    data: streams ? { streams } : {},
  });
}

export async function discoverSchema(id: number) {
  return request<DiscoveredSchema>(`${API_PREFIX}/${id}:discoverSchema`, {
    method: 'POST',
  });
}

export async function getSchemaChanges(id: number) {
  return request<SchemaChange>(`${API_PREFIX}/${id}/schemaChanges`, {
    method: 'GET',
  });
}

export async function applySchemaChanges(id: number, catalog: ConfiguredCatalog) {
  return request<void>(`${API_PREFIX}/${id}:applySchemaChanges`, {
    method: 'POST',
    data: catalog,
  });
}

export async function getTimeline(
  id: number,
  params: { current?: number; pageSize?: number; eventType?: string },
) {
  return request<PageResult<ConnectionEventDO>>(`${API_PREFIX}/${id}/timeline`, {
    method: 'GET',
    params: {
      current: params.current || 1,
      pageSize: params.pageSize || 20,
      eventType: params.eventType,
    },
  });
}

export async function getJobHistory(id: number, params: { current?: number; pageSize?: number }) {
  return request<PageResult<DataSyncExecutionDO>>(`${API_PREFIX}/${id}/jobs`, {
    method: 'GET',
    params: {
      current: params.current || 1,
      pageSize: params.pageSize || 20,
    },
  });
}
