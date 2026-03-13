import request from '@/services/request';

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
  return request.post<ConnectionDO>(API_PREFIX, {
    data: connection,
  });
}

export async function updateConnection(id: number, connection: Partial<ConnectionDO>) {
  return request.patch<ConnectionDO>(`${API_PREFIX}/${id}`, {
    data: connection,
  });
}

export async function deleteConnection(id: number) {
  return request.delete<void>(`${API_PREFIX}/${id}`);
}

export async function getConnectionById(id: number) {
  return request.get<ConnectionDO>(`${API_PREFIX}/${id}`);
}

export async function listConnections(params: {
  current?: number;
  pageSize?: number;
  sourceDbId?: number;
  destDbId?: number;
  status?: string;
}) {
  return request.get<PageResult<ConnectionDO>>(API_PREFIX, {
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
  return request.post<void>(`${API_PREFIX}/${id}:pause`);
}

export async function resumeConnection(id: number) {
  return request.post<void>(`${API_PREFIX}/${id}:resume`);
}

export async function deprecateConnection(id: number, reason?: string) {
  return request.post<void>(`${API_PREFIX}/${id}:deprecate`, {
    data: reason ? { reason } : {},
  });
}

export async function triggerSync(id: number) {
  return request.post<void>(`${API_PREFIX}/${id}:sync`);
}

export async function resetState(id: number, streams?: string[]) {
  return request.post<void>(`${API_PREFIX}/${id}:resetState`, {
    data: streams ? { streams } : {},
  });
}

export async function discoverSchema(id: number) {
  return request.post<DiscoveredSchema>(`${API_PREFIX}/${id}:discoverSchema`);
}

export async function getSchemaChanges(id: number) {
  return request.get<SchemaChange>(`${API_PREFIX}/${id}/schemaChanges`);
}

export async function applySchemaChanges(id: number, catalog: ConfiguredCatalog) {
  return request.post<void>(`${API_PREFIX}/${id}:applySchemaChanges`, {
    data: catalog,
  });
}

export async function getTimeline(
  id: number,
  params: { current?: number; pageSize?: number; eventType?: string },
) {
  return request.get<PageResult<ConnectionEventDO>>(`${API_PREFIX}/${id}/timeline`, {
    params: {
      current: params.current || 1,
      pageSize: params.pageSize || 20,
      eventType: params.eventType,
    },
  });
}

export async function getJobHistory(id: number, params: { current?: number; pageSize?: number }) {
  return request.get<PageResult<DataSyncExecutionDO>>(`${API_PREFIX}/${id}/jobs`, {
    params: {
      current: params.current || 1,
      pageSize: params.pageSize || 20,
    },
  });
}
