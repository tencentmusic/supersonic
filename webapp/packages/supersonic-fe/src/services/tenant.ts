import request from './request';

// 租户相关类型定义
export interface Tenant {
  id: number;
  name: string;
  code: string;
  description?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  contactEmail?: string;
  contactName?: string;
  contactPhone?: string;
  logoUrl?: string;
  settings?: string;
  /** Enriched at query time, not persisted */
  subscriptionPlanName?: string;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface TenantUsage {
  id?: number;
  tenantId: number;
  usageDate: string;
  apiCalls: number;
  tokensUsed: number;
  queryCount: number;
  storageBytes: number;
  activeUsers: number;
}

export interface TenantSubscription {
  id: number;
  tenantId: number;
  planId: number;
  planName?: string;
  status: 'ACTIVE' | 'CANCELLED' | 'EXPIRED' | 'PENDING';
  startDate: string;
  endDate?: string;
  billingCycle: 'MONTHLY' | 'YEARLY';
  lastPaymentDate?: string;
  nextPaymentDate?: string;
  paymentMethod?: string;
}

export interface SubscriptionPlan {
  id: number;
  name: string;
  code: string;
  description?: string;
  priceMonthly?: number;
  priceYearly?: number;
  maxUsers: number;
  maxDatasets: number;
  maxModels: number;
  maxAgents: number;
  maxApiCallsPerDay: number;
  maxTokensPerMonth: number;
  features?: string;
  status: 'ACTIVE' | 'INACTIVE';
}

// 当前租户相关API
export async function getCurrentTenant(): Promise<Result<Tenant>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}tenant/current`);
}

export async function updateCurrentTenant(data: Partial<Tenant>): Promise<Result<Tenant>> {
  return request.put(`${process.env.AUTH_API_BASE_URL}tenant/current`, { data });
}

export async function getTenantUsageToday(): Promise<Result<TenantUsage>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}tenant/usage/today`);
}

export async function getTenantUsageRange(
  startDate: string,
  endDate: string,
): Promise<Result<TenantUsage[]>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}tenant/usage/range`, {
    params: { startDate, endDate },
  });
}

export async function getTenantUsageMonthly(
  year: number,
  month: number,
): Promise<Result<TenantUsage>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}tenant/usage/monthly`, {
    params: { year, month },
  });
}

export async function checkTenantCode(code: string): Promise<Result<boolean>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}tenant/check-code`, {
    params: { code },
  });
}

// 管理员租户管理API
export async function getAllTenants(): Promise<Result<Tenant[]>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}admin/tenant`);
}

export async function getActiveTenants(): Promise<Result<Tenant[]>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}admin/tenant/active`);
}

export async function getTenantById(id: number): Promise<Result<Tenant>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}admin/tenant/${id}`);
}

export async function getTenantByCode(code: string): Promise<Result<Tenant>> {
  return request.get(`${process.env.AUTH_API_BASE_URL}admin/tenant/code/${code}`);
}

export async function createTenant(data: Partial<Tenant>): Promise<Result<Tenant>> {
  return request.post(`${process.env.AUTH_API_BASE_URL}admin/tenant`, { data });
}

export async function updateTenant(id: number, data: Partial<Tenant>): Promise<Result<Tenant>> {
  return request.put(`${process.env.AUTH_API_BASE_URL}admin/tenant/${id}`, { data });
}

export async function deleteTenant(id: number): Promise<Result<void>> {
  return request.delete(`${process.env.AUTH_API_BASE_URL}admin/tenant/${id}`);
}

export async function suspendTenant(id: number): Promise<Result<Tenant>> {
  return request.post(`${process.env.AUTH_API_BASE_URL}admin/tenant/${id}/suspend`);
}

export async function activateTenant(id: number): Promise<Result<Tenant>> {
  return request.post(`${process.env.AUTH_API_BASE_URL}admin/tenant/${id}/activate`);
}

// ========== 租户角色管理 ==========

export async function getTenantRoles(): Promise<Result<any[]>> {
  return request.get<Result<any[]>>(`${process.env.AUTH_API_BASE_URL}role/scope/TENANT`, {
    params: { tenantId: 1 },
  });
}

export async function getTenantRole(id: number): Promise<Result<any>> {
  return request.get<Result<any>>(`${process.env.AUTH_API_BASE_URL}role/${id}`);
}

export async function getRolePermissionIds(roleId: number): Promise<Result<number[]>> {
  return request.get<Result<number[]>>(`${process.env.AUTH_API_BASE_URL}role/${roleId}/permission-ids`);
}

export async function createTenantRole(roleData: any): Promise<Result<any>> {
  return request.post<Result<any>>(`${process.env.AUTH_API_BASE_URL}role`, {
    data: { ...roleData, scope: 'TENANT', tenantId: 1 },
  });
}

export async function updateTenantRole(id: number, roleData: any): Promise<Result<any>> {
  return request.put<Result<any>>(`${process.env.AUTH_API_BASE_URL}role`, {
    data: { ...roleData, id, scope: 'TENANT' },
  });
}

export async function deleteTenantRole(id: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${process.env.AUTH_API_BASE_URL}role/${id}`);
}

export async function assignPermissionsToTenantRole(
  roleId: number,
  permissionIds: number[],
): Promise<Result<void>> {
  return request.put<Result<void>>(`${process.env.AUTH_API_BASE_URL}role/${roleId}/permissions`, {
    data: permissionIds,
  });
}

export async function assignRoleToUser(data: {
  userId: number;
  roleIds: number[];
}): Promise<Result<void>> {
  return request.post<Result<void>>(`${process.env.AUTH_API_BASE_URL}user/role`, { data });
}

// ========== 租户权限管理 ==========

export async function getTenantPermissions(): Promise<Result<any[]>> {
  return request.get<Result<any[]>>(`${process.env.AUTH_API_BASE_URL}permission/scope/TENANT`);
}

export async function getTenantPermission(id: number): Promise<Result<any>> {
  return request.get<Result<any>>(`${process.env.AUTH_API_BASE_URL}permission/${id}`);
}

export async function createTenantPermission(permData: any): Promise<Result<any>> {
  return request.post<Result<any>>(`${process.env.AUTH_API_BASE_URL}permission`, {
    data: { ...permData, scope: 'TENANT' },
  });
}

export async function updateTenantPermission(id: number, permData: any): Promise<Result<any>> {
  return request.put<Result<any>>(`${process.env.AUTH_API_BASE_URL}permission`, {
    data: { ...permData, id, scope: 'TENANT' },
  });
}

export async function deleteTenantPermission(id: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${process.env.AUTH_API_BASE_URL}permission/${id}`);
}

// ========== 租户设置 ==========

export async function getTenantSettings(): Promise<Result<any>> {
  return request.get(`${process.env.API_BASE_URL}tenant/settings`);
}

export async function updateTenantSettings(data: any): Promise<Result<any>> {
  return request.put(`${process.env.API_BASE_URL}tenant/settings`, { data });
}