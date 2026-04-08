import request, { getStoredTenantIdNumber } from './request';

/**
 * Platform Admin API Services
 *
 * API Design follows Google RESTful API guidelines:
 * - Version: v1 (major version in URL path)
 * - No module prefix (removed /auth/)
 */

const API_V1 = '/api/v1';

// ========== 租户管理 ==========
// TODO: Migrate to /api/v1/admin/tenants

const AUTH_API_BASE = process.env.AUTH_API_BASE_URL || '/api/auth/';

export async function getAllTenants(): Promise<Result<any[]>> {
  return request.get<Result<any[]>>(`${AUTH_API_BASE}admin/tenant`);
}

export async function getTenant(id: number): Promise<Result<any>> {
  return request.get<Result<any>>(`${AUTH_API_BASE}admin/tenant/${id}`);
}

export async function createTenant(tenantData: any): Promise<Result<any>> {
  return request.post<Result<any>>(`${AUTH_API_BASE}admin/tenant`, { data: tenantData });
}

export async function updateTenant(id: number, tenantData: any): Promise<Result<any>> {
  return request.put<Result<any>>(`${AUTH_API_BASE}admin/tenant/${id}`, { data: tenantData });
}

export async function deleteTenant(id: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${AUTH_API_BASE}admin/tenant/${id}`);
}

// ========== 订阅计划管理 ==========
// Resource: /api/v1/subscription-plans (admin operations with @PreAuthorize)

export async function getSubscriptionPlans(): Promise<Result<any[]>> {
  return request.get<Result<any[]>>(`${API_V1}/subscription-plans/all`);
}

export async function getSubscriptionPlan(planId: number): Promise<Result<any>> {
  return request.get<Result<any>>(`${API_V1}/subscription-plans/${planId}`);
}

export async function createSubscriptionPlan(planData: any): Promise<Result<any>> {
  return request.post<Result<any>>(`${API_V1}/subscription-plans`, { data: planData });
}

export async function updateSubscriptionPlan(planId: number, planData: any): Promise<Result<any>> {
  return request.put<Result<any>>(`${API_V1}/subscription-plans/${planId}`, { data: planData });
}

export async function deleteSubscriptionPlan(planId: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${API_V1}/subscription-plans/${planId}`);
}

// ========== 租户订阅管理 ==========
// Resource: /api/v1/tenants/{tenantId}/subscription (admin operations with @PreAuthorize)

export async function getTenantSubscription(tenantId: number): Promise<Result<any>> {
  return request.get<Result<any>>(`${API_V1}/tenants/${tenantId}/subscription`);
}

export async function listTenantSubscriptions(tenantId: number): Promise<Result<any[]>> {
  return request.get<Result<any[]>>(`${API_V1}/tenants/${tenantId}/subscriptions`);
}

export async function assignTenantSubscription(
  tenantId: number,
  data: { planId: number; billingCycle: string },
): Promise<Result<any>> {
  return request.put<Result<any>>(`${API_V1}/tenants/${tenantId}/subscription`, { data });
}

export async function cancelTenantSubscription(tenantId: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${API_V1}/tenants/${tenantId}/subscription`);
}

// ========== 平台角色管理 ==========

export async function getPlatformRoles(): Promise<Result<any[]>> {
  return request.get<Result<any[]>>(`${AUTH_API_BASE}role/scope/PLATFORM`, {
    params: { tenantId: getStoredTenantIdNumber() },
  });
}

export async function getPlatformRole(id: number): Promise<Result<any>> {
  return request.get<Result<any>>(`${AUTH_API_BASE}role/${id}`);
}

export async function getRolePermissionIds(roleId: number): Promise<Result<number[]>> {
  return request.get<Result<number[]>>(`${AUTH_API_BASE}role/${roleId}/permission-ids`);
}

export async function createPlatformRole(roleData: any): Promise<Result<any>> {
  return request.post<Result<any>>(`${AUTH_API_BASE}role`, {
    data: { ...roleData, scope: 'PLATFORM', tenantId: getStoredTenantIdNumber() },
  });
}

export async function updatePlatformRole(id: number, roleData: any): Promise<Result<any>> {
  return request.put<Result<any>>(`${AUTH_API_BASE}role`, {
    data: { ...roleData, id, scope: 'PLATFORM' },
  });
}

export async function deletePlatformRole(id: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${AUTH_API_BASE}role/${id}`);
}

export async function assignPermissionsToRole(roleId: number, permissionIds: number[]): Promise<Result<void>> {
  return request.put<Result<void>>(`${AUTH_API_BASE}role/${roleId}/permissions`, {
    data: permissionIds,
  });
}

// ========== 平台权限管理 ==========

export async function getPlatformPermissions(): Promise<Result<any[]>> {
  return request.get<Result<any[]>>(`${AUTH_API_BASE}permission/scope/PLATFORM`);
}

export async function getPlatformPermission(id: number): Promise<Result<any>> {
  return request.get<Result<any>>(`${AUTH_API_BASE}permission/${id}`);
}

export async function createPlatformPermission(permData: any): Promise<Result<any>> {
  return request.post<Result<any>>(`${AUTH_API_BASE}permission`, {
    data: { ...permData, scope: 'PLATFORM' },
  });
}

export async function updatePlatformPermission(id: number, permData: any): Promise<Result<any>> {
  return request.put<Result<any>>(`${AUTH_API_BASE}permission`, {
    data: { ...permData, id, scope: 'PLATFORM' },
  });
}

export async function deletePlatformPermission(id: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${AUTH_API_BASE}permission/${id}`);
}
