import request from './request';

const BASE = '/api/v1/dataSyncConfigs';

export interface DataSyncConfig {
  id: number;
  name: string;
  sourceDatabaseId: number;
  targetDatabaseId: number;
  syncConfig?: string;
  cronExpression: string;
  retryCount: number;
  enabled: boolean;
  quartzJobKey?: string;
  createdBy?: string;
  tenantId?: number;
  createdAt?: string;
}

export interface DataSyncExecution {
  id: number;
  syncConfigId: number;
  status: string;
  startTime?: string;
  endTime?: string;
  rowsRead?: number;
  rowsWritten?: number;
  watermarkValue?: string;
  errorMessage?: string;
}

export function getSyncConfigList(params?: { current?: number; pageSize?: number }) {
  return request(BASE, { method: 'GET', params });
}

export function getSyncConfigById(id: number) {
  return request(`${BASE}/${id}`, { method: 'GET' });
}

export function createSyncConfig(data: Partial<DataSyncConfig>) {
  return request(BASE, { method: 'POST', data });
}

export function updateSyncConfig(id: number, data: Partial<DataSyncConfig>) {
  return request(`${BASE}/${id}`, { method: 'PATCH', data });
}

export function deleteSyncConfig(id: number) {
  return request(`${BASE}/${id}`, { method: 'DELETE' });
}

export function triggerSync(id: number) {
  return request(`${BASE}/${id}:trigger`, { method: 'POST' });
}

export function pauseSync(id: number) {
  return request(`${BASE}/${id}:pause`, { method: 'POST' });
}

export function resumeSync(id: number) {
  return request(`${BASE}/${id}:resume`, { method: 'POST' });
}

export function getSyncExecutionList(
  configId: number,
  params?: { current?: number; pageSize?: number },
) {
  return request(`${BASE}/${configId}/executions`, { method: 'GET', params });
}

export function discoverSchema(configId: number) {
  return request(`${BASE}/${configId}:discoverSchema`, { method: 'POST' });
}

/**
 * Discover schema for a database (used in wizard before config is created).
 */
export function discoverSchemaByDatabase(databaseId: number) {
  return request(`${BASE}:discoverSchemaByDatabase`, {
    method: 'POST',
    params: { databaseId },
  });
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
  nullable?: boolean;
}

export interface DiscoveredSchema {
  tables: DiscoveredTable[];
}

export type SyncMode = 'FULL' | 'INCREMENTAL' | 'INCREMENTAL_DEDUP' | 'PARTITION_OVERWRITE';

export interface TableSyncConfig {
  sourceTable: string;
  targetTable: string;
  syncMode: SyncMode;
  cursorField?: string;
  primaryKey?: string;
  selectedFields?: string[];
  batchSize?: number;
}
