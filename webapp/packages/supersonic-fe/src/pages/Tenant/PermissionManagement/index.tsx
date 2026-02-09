import React from 'react';
import ScopePermissionManagement from '@/components/ScopePermissionManagement';
import {
  getTenantPermissions,
  createTenantPermission,
  updateTenantPermission,
  deleteTenantPermission,
} from '@/services/tenant';

const TenantPermissionManagement: React.FC = () => (
  <ScopePermissionManagement
    scope="TENANT"
    title="租户权限管理"
    scopeTag={{ color: 'blue', text: '租户级' }}
    codePlaceholder="如: TENANT_ORG_MANAGE"
    namePlaceholder="如: 组织架构管理"
    api={{
      getList: getTenantPermissions,
      create: createTenantPermission,
      update: updateTenantPermission,
      delete: deleteTenantPermission,
    }}
  />
);

export default TenantPermissionManagement;
