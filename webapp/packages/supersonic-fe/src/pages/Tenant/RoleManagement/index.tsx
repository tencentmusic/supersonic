import React from 'react';
import ScopeRoleManagement from '@/components/ScopeRoleManagement';
import {
  getTenantRoles,
  createTenantRole,
  updateTenantRole,
  deleteTenantRole,
  getTenantPermissions,
  assignPermissionsToTenantRole,
  getRolePermissionIds,
} from '@/services/tenant';

const TenantRoleManagement: React.FC = () => (
  <ScopeRoleManagement
    scope="TENANT"
    title="租户角色管理"
    scopeTag={{ color: 'blue', text: '租户级' }}
    codePlaceholder="如: TENANT_ADMIN, ANALYST, VIEWER"
    namePlaceholder="如: 租户管理员, 分析师, 查看者"
    api={{
      getRoles: getTenantRoles,
      createRole: createTenantRole,
      updateRole: updateTenantRole,
      deleteRole: deleteTenantRole,
      getPermissions: getTenantPermissions,
      assignPermissions: assignPermissionsToTenantRole,
      getRolePermissionIds,
    }}
  />
);

export default TenantRoleManagement;
