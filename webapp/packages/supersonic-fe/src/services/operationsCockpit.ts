import request from './request';

export interface CockpitTopicSummary {
  id: number;
  name: string;
  description?: string;
  priority?: number;
  fixedReportCount: number;
  alertRuleCount: number;
  scheduleCount: number;
}

export interface CockpitFixedReportSummary {
  datasetId: number;
  reportName: string;
  domainName?: string;
  consumptionStatus?: string;
  latestResultTime?: string;
  subscribed: boolean;
}

export interface CockpitAlertEventSummary {
  id: number;
  ruleId: number;
  severity?: string;
  resolutionStatus: string;
  message?: string;
  createdAt?: string;
}

export interface OperationsCockpitData {
  topics: CockpitTopicSummary[];
  keyReports: CockpitFixedReportSummary[];
  pendingAlertEvents: CockpitAlertEventSummary[];
  reliabilityRisks: CockpitFixedReportSummary[];
  pendingAlertEventCount: number;
}

export function getOperationsCockpit(): Promise<OperationsCockpitData> {
  return request('/api/v1/operations/cockpit', { method: 'GET' });
}
