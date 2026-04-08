import type { DeliveryType } from './deliveryConfig';
import request from './request';

export interface DeliverySummaryItem {
  configId: number;
  configName: string;
  deliveryType: DeliveryType;
  enabled: boolean;
}

export interface FixedReport {
  deploymentId: number;
  datasetId: number;
  reportName: string;
  description?: string;
  domainName?: string;
  latestResultTime?: string;
  latestResultStatus?: string;
  latestErrorMessage?: string;
  latestRowCount?: number;
  resultExpired: boolean;
  previousSuccessTime?: string;
  scheduleCount: number;
  enabledScheduleCount: number;
  deliveryChannels: DeliverySummaryItem[];
  subscribed: boolean;
  consumptionStatus: string;
}

const BASE = '/api/v1/fixedReports';

export function getFixedReports(params?: {
  keyword?: string;
  domainName?: string;
  status?: string;
  view?: string;
}): Promise<FixedReport[]> {
  return request(BASE, { method: 'GET', params });
}

export function subscribe(datasetId: number): Promise<void> {
  return request(`${BASE}/${datasetId}/subscription`, { method: 'POST' });
}

export function unsubscribe(datasetId: number): Promise<void> {
  return request(`${BASE}/${datasetId}/subscription`, { method: 'DELETE' });
}

export function getReportExecutions(
  datasetId: number,
  params?: { current?: number; pageSize?: number },
) {
  return request(`${BASE}/${datasetId}/executions`, { method: 'GET', params });
}
