import request from './request';

export interface TopicItem {
  itemId: number;
  itemType: 'FIXED_REPORT' | 'ALERT_RULE' | 'SCHEDULE';
  itemName: string;
  /** 后端聚合：固定报表消费状态 */
  consumptionStatus?: string;
  /** 后端聚合：定时任务是否启用 */
  scheduleEnabled?: boolean;
}

export interface BusinessTopic {
  id?: number;
  name: string;
  description?: string;
  priority?: number;
  ownerId?: number;
  ownerName?: string;
  defaultDeliveryConfigIds?: string;
  enabled?: number;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;

  fixedReportCount?: number;
  alertRuleCount?: number;
  scheduleCount?: number;
  items?: TopicItem[];
}

const BASE = '/api/v1/businessTopics';

export function getTopicList(params?: { current?: number; pageSize?: number; enabled?: boolean }) {
  return request(BASE, { method: 'GET', params });
}

export function getTopicDetail(id: number) {
  return request(`${BASE}/${id}`, { method: 'GET' });
}

export function createTopic(data: Partial<BusinessTopic>) {
  return request(BASE, { method: 'POST', data });
}

export function updateTopic(id: number, data: Partial<BusinessTopic>) {
  return request(`${BASE}/${id}`, { method: 'PATCH', data });
}

export function deleteTopic(id: number) {
  return request(`${BASE}/${id}`, { method: 'DELETE' });
}

export function addTopicItems(
  topicId: number,
  itemTypes: string[],
  itemIds: number[],
) {
  return request(`${BASE}/${topicId}/items`, {
    method: 'POST',
    data: { itemTypes, itemIds },
  });
}

export function removeTopicItem(topicId: number, itemType: string, itemId: number) {
  return request(`${BASE}/${topicId}/items/${itemType}/${itemId}`, { method: 'DELETE' });
}
