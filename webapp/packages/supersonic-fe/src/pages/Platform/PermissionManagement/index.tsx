import React from 'react';
import ScopePermissionManagement from '@/components/ScopePermissionManagement';
import {
  getPlatformPermissions,
  createPlatformPermission,
  updatePlatformPermission,
  deletePlatformPermission,
} from '@/services/platform';

const PlatformPermissionManagement: React.FC = () => (
  <ScopePermissionManagement
    scope="PLATFORM"
    title="平台权限管理"
    scopeTag={{ color: 'purple', text: '平台级' }}
    codePlaceholder="如: PLATFORM_TENANT_MANAGE"
    namePlaceholder="如: 租户管理"
    api={{
      getList: getPlatformPermissions,
      create: createPlatformPermission,
      update: updatePlatformPermission,
      delete: deletePlatformPermission,
    }}
  />
);

export default PlatformPermissionManagement;
