import request from '@/services/request';

const BASE = '/api/v1/report-executions';

export interface DeliveryRecord {
  channelType: string;
  status: string;
  deliveredAt?: string;
}

export interface ExecutionSnapshot {
  executionId: number;
  templateName: string;
  templateVersion: number;
  triggerType: string;
  executedAt: string;
  durationMs: number;
  status: string;
  params: Record<string, string>;
  sql: string;
  resultRowCount: number;
  resultPreview: Record<string, any>[];
  deliveryRecords: DeliveryRecord[];
}

export async function getExecutionSnapshot(executionId: number): Promise<ExecutionSnapshot> {
  const res = await request(`${BASE}/${executionId}/snapshot`, { method: 'GET' });
  return res?.data ?? res;
}
