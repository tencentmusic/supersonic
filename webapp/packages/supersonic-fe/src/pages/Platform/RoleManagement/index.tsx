import React from 'react';
import ScopeRoleManagement from '@/components/ScopeRoleManagement';
import {
  getPlatformRoles,
  createPlatformRole,
  updatePlatformRole,
  deletePlatformRole,
  getPlatformPermissions,
  assignPermissionsToRole,
  getRolePermissionIds,
} from '@/services/platform';

const PlatformRoleManagement: React.FC = () => (
  <ScopeRoleManagement
    scope="PLATFORM"
    title="平台角色管理"
    scopeTag={{ color: 'purple', text: '平台级' }}
    codePlaceholder="如: PLATFORM_ADMIN, PLATFORM_OPERATOR"
    namePlaceholder="如: 平台管理员, 平台运营"
    api={{
      getRoles: getPlatformRoles,
      createRole: createPlatformRole,
      updateRole: updatePlatformRole,
      deleteRole: deletePlatformRole,
      getPermissions: getPlatformPermissions,
      assignPermissions: assignPermissionsToRole,
      getRolePermissionIds,
    }}
  />
);

export default PlatformRoleManagement;
