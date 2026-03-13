import request from './request';

export interface Role {
  id?: number;
  name: string;
  code: string;
  description?: string;
  scope?: 'PLATFORM' | 'TENANT';
  tenantId?: number;
  isSystem?: boolean;
  status?: boolean;
  permissionIds?: number[];
  permissionCodes?: string[];
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface Permission {
  id?: number;
  name: string;
  code: string;
  type: 'MENU' | 'BUTTON' | 'API' | 'DATA';
  scope?: 'PLATFORM' | 'TENANT';
  parentId?: number;
  path?: string;
  icon?: string;
  sortOrder?: number;
  description?: string;
  status?: boolean;
  children?: Permission[];
}

// 获取角色列表
export async function getRoleList(tenantId: number = 1): Promise<Result<Role[]>> {
  return request.get<Result<Role[]>>(`${process.env.AUTH_API_BASE_URL}role/list`, {
    params: { tenantId },
  });
}

// 根据 scope 获取角色列表
export async function getRolesByScope(scope: 'PLATFORM' | 'TENANT', tenantId: number = 1): Promise<Result<Role[]>> {
  return request.get<Result<Role[]>>(`${process.env.AUTH_API_BASE_URL}role/scope/${scope}`, {
    params: { tenantId },
  });
}

// 根据ID获取角色
export async function getRoleById(id: number): Promise<Result<Role>> {
  return request.get<Result<Role>>(`${process.env.AUTH_API_BASE_URL}role/${id}`);
}

// 创建角色
export async function createRole(role: Role): Promise<Result<Role>> {
  return request.post<Result<Role>>(`${process.env.AUTH_API_BASE_URL}role`, {
    data: role,
  });
}

// 更新角色
export async function updateRole(role: Role): Promise<Result<Role>> {
  return request.put<Result<Role>>(`${process.env.AUTH_API_BASE_URL}role`, {
    data: role,
  });
}

// 删除角色
export async function deleteRole(id: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${process.env.AUTH_API_BASE_URL}role/${id}`);
}

// 更新角色权限
export async function updateRolePermissions(
  roleId: number,
  permissionIds: number[],
): Promise<Result<void>> {
  return request.put<Result<void>>(`${process.env.AUTH_API_BASE_URL}role/${roleId}/permissions`, {
    data: permissionIds,
  });
}

// 获取角色的权限ID列表
export async function getRolePermissionIds(roleId: number): Promise<Result<number[]>> {
  return request.get<Result<number[]>>(`${process.env.AUTH_API_BASE_URL}role/${roleId}/permission-ids`);
}

// 获取所有权限列表
export async function getAllPermissions(): Promise<Result<Permission[]>> {
  return request.get<Result<Permission[]>>(`${process.env.AUTH_API_BASE_URL}permission/list`);
}

// 根据 scope 获取权限列表
export async function getPermissionsByScope(scope: 'PLATFORM' | 'TENANT'): Promise<Result<Permission[]>> {
  return request.get<Result<Permission[]>>(`${process.env.AUTH_API_BASE_URL}permission/scope/${scope}`);
}

// 获取权限树
export async function getPermissionTree(): Promise<Result<Permission[]>> {
  return request.get<Result<Permission[]>>(`${process.env.AUTH_API_BASE_URL}permission/tree`);
}

// 获取当前用户权限
export async function getCurrentUserPermissions(): Promise<Result<string[]>> {
  return request.get<Result<string[]>>(`${process.env.AUTH_API_BASE_URL}permission/current`);
}

// 创建权限
export async function createPermission(permission: Permission): Promise<Result<Permission>> {
  return request.post<Result<Permission>>(`${process.env.AUTH_API_BASE_URL}permission`, {
    data: permission,
  });
}

// 更新权限
export async function updatePermission(permission: Permission): Promise<Result<Permission>> {
  return request.put<Result<Permission>>(`${process.env.AUTH_API_BASE_URL}permission`, {
    data: permission,
  });
}

// 删除权限
export async function deletePermission(id: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${process.env.AUTH_API_BASE_URL}permission/${id}`);
}
