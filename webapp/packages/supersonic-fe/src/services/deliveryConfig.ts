import tRequest from './request';

const BASE = '/api/semantic/delivery';

// ========== Type Definitions ==========

export type DeliveryType = 'EMAIL' | 'WEBHOOK' | 'FEISHU' | 'DINGTALK' | 'WECHAT_WORK';
export type DeliveryStatus = 'PENDING' | 'SENDING' | 'SUCCESS' | 'FAILED';

export interface DeliveryConfig {
  id: number;
  name: string;
  deliveryType: DeliveryType;
  deliveryConfig?: string; // JSON config
  enabled: boolean;
  description?: string;
  consecutiveFailures?: number;
  maxConsecutiveFailures?: number;
  tenantId?: number;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface DeliveryRecord {
  id: number;
  deliveryKey: string;
  scheduleId: number;
  executionId?: number;
  configId: number;
  deliveryType: DeliveryType;
  status: DeliveryStatus;
  fileLocation?: string;
  errorMessage?: string;
  retryCount: number;
  startedAt?: string;
  completedAt?: string;
  tenantId?: number;
  createdAt?: string;
}

// Email channel config
export interface EmailConfig {
  from?: string;
  to: string[];
  cc?: string[];
  subject?: string;
  body?: string;
  htmlBody?: boolean;
}

// Webhook channel config
export interface WebhookConfig {
  url: string;
  method?: 'POST' | 'PUT';
  headers?: Record<string, string>;
  includeFile?: boolean;
  bodyTemplate?: string;
}

// Feishu channel config
export interface FeishuConfig {
  webhookUrl: string;
  secret?: string;
  msgType?: 'post' | 'interactive';
  title?: string;
  downloadUrl?: string;
}

// DingTalk channel config
export interface DingtalkConfig {
  webhookUrl: string;
  secret?: string;
  msgType?: 'markdown' | 'actionCard';
  title?: string;
  downloadUrl?: string;
  atMobiles?: string[];
}

// WeChatWork channel config
export interface WeChatWorkConfig {
  webhookUrl: string;
  msgType?: 'text' | 'markdown' | 'news';
  title?: string;
  downloadUrl?: string;
}

// ========== Config CRUD APIs ==========

export function getConfigList(params?: { pageNum?: number; pageSize?: number }) {
  return tRequest(`${BASE}/configs`, {
    method: 'GET',
    params: {
      pageNum: params?.pageNum || 1,
      pageSize: params?.pageSize || 20,
    },
  });
}

export function getConfigById(id: number) {
  return tRequest(`${BASE}/configs/${id}`, {
    method: 'GET',
  });
}

export function createConfig(data: Partial<DeliveryConfig>) {
  return tRequest(`${BASE}/configs`, {
    method: 'POST',
    data,
  });
}

export function updateConfig(id: number, data: Partial<DeliveryConfig>) {
  return tRequest(`${BASE}/configs/${id}`, {
    method: 'PUT',
    data,
  });
}

export function deleteConfig(id: number) {
  return tRequest(`${BASE}/configs/${id}`, {
    method: 'DELETE',
  });
}

// ========== Test Delivery ==========

export function testConfig(id: number) {
  return tRequest(`${BASE}/configs/${id}:test`, {
    method: 'POST',
  });
}

// ========== Record APIs ==========

export function getRecordList(params?: {
  pageNum?: number;
  pageSize?: number;
  scheduleId?: number;
  executionId?: number;
  configId?: number;
}) {
  return tRequest(`${BASE}/records`, {
    method: 'GET',
    params: {
      pageNum: params?.pageNum || 1,
      pageSize: params?.pageSize || 20,
      scheduleId: params?.scheduleId,
      executionId: params?.executionId,
    },
  });
}

export function retryRecord(id: number) {
  return tRequest(`${BASE}/records/${id}:retry`, {
    method: 'POST',
  });
}

// ========== Constants ==========

export const DELIVERY_TYPE_OPTIONS = [
  { label: '邮件', value: 'EMAIL' },
  { label: 'Webhook', value: 'WEBHOOK' },
  { label: '飞书', value: 'FEISHU' },
  { label: '钉钉', value: 'DINGTALK' },
  { label: '企业微信', value: 'WECHAT_WORK' },
];

export const DELIVERY_STATUS_MAP: Record<DeliveryStatus, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '等待中' },
  SENDING: { color: 'blue', text: '发送中' },
  SUCCESS: { color: 'green', text: '成功' },
  FAILED: { color: 'red', text: '失败' },
};

export const DELIVERY_TYPE_MAP: Record<DeliveryType, { color: string; text: string }> = {
  EMAIL: { color: 'blue', text: '邮件' },
  WEBHOOK: { color: 'purple', text: 'Webhook' },
  FEISHU: { color: 'cyan', text: '飞书' },
  DINGTALK: { color: 'orange', text: '钉钉' },
  WECHAT_WORK: { color: 'green', text: '企业微信' },
};

// ========== Statistics Types ==========

export interface DeliveryStatistics {
  totalDeliveries: number;
  successCount: number;
  failedCount: number;
  pendingCount: number;
  successRate: number;
  countByType: Record<string, number>;
  successRateByType: Record<string, number>;
  avgDeliveryTimeMs: number;
}

export interface DailyDeliveryStats {
  date: string;
  total: number;
  success: number;
  failed: number;
  successRate: number;
}

// ========== Statistics APIs ==========

export function getStatistics(days = 7) {
  return tRequest(`${BASE}/statistics`, {
    method: 'GET',
    params: { days },
  });
}

export function getDailyStats(days = 7) {
  return tRequest(`${BASE}/statistics/daily`, {
    method: 'GET',
    params: { days },
  });
}
