package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Permission;
import com.tencent.supersonic.auth.api.authentication.pojo.Role;
import com.tencent.supersonic.auth.api.authentication.service.RoleService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.PermissionDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.RoleDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.RolePermissionDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.PermissionDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.RoleDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.RolePermissionDOMapper;
import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleDOMapper roleDOMapper;
    private final PermissionDOMapper permissionDOMapper;
    private final RolePermissionDOMapper rolePermissionDOMapper;

    public RoleServiceImpl(RoleDOMapper roleDOMapper, PermissionDOMapper permissionDOMapper,
            RolePermissionDOMapper rolePermissionDOMapper) {
        this.roleDOMapper = roleDOMapper;
        this.permissionDOMapper = permissionDOMapper;
        this.rolePermissionDOMapper = rolePermissionDOMapper;
    }

    @Override
    public List<Role> getRoleList(Long tenantId) {
        List<RoleDO> roleDOList = roleDOMapper.selectByTenantId(tenantId);
        return roleDOList.stream().map(this::convertToRole).collect(Collectors.toList());
    }

    @Override
    public List<Role> getRolesByScope(String scope, Long tenantId) {
        List<RoleDO> roleDOList = roleDOMapper.selectByScopeAndTenantId(scope, tenantId);
        return roleDOList.stream().map(this::convertToRole).collect(Collectors.toList());
    }

    @Override
    public Role getRoleById(Long id) {
        RoleDO roleDO = roleDOMapper.selectById(id);
        if (roleDO == null) {
            return null;
        }
        Role role = convertToRole(roleDO);
        // 加载权限
        List<PermissionDO> permissions = permissionDOMapper.selectByRoleId(id);
        role.setPermissionIds(
                permissions.stream().map(PermissionDO::getId).collect(Collectors.toList()));
        role.setPermissionCodes(
                permissions.stream().map(PermissionDO::getCode).collect(Collectors.toList()));
        return role;
    }

    @Override
    public Role getRoleByCode(String code, Long tenantId) {
        RoleDO roleDO = roleDOMapper.selectByCodeAndTenantId(code, tenantId);
        return roleDO == null ? null : convertToRole(roleDO);
    }

    @Override
    @Transactional
    public Role createRole(Role role, String operator) {
        RoleDO roleDO = convertToRoleDO(role);
        roleDO.setCreatedAt(new Date());
        roleDO.setCreatedBy(operator);
        roleDO.setUpdatedAt(new Date());
        roleDO.setUpdatedBy(operator);
        roleDOMapper.insert(roleDO);
        role.setId(roleDO.getId());

        // 保存角色权限关联
        if (!CollectionUtils.isEmpty(role.getPermissionIds())) {
            updateRolePermissions(roleDO.getId(), role.getPermissionIds(), operator);
        }

        return role;
    }

    @Override
    @Transactional
    public Role updateRole(Role role, String operator) {
        RoleDO roleDO = convertToRoleDO(role);
        // Guard: prevent cross-tenant mutation of TENANT-scope roles.
        // s2_role is excluded from TenantSqlInterceptor so we enforce the boundary here.
        if (roleDO.getId() != null) {
            RoleDO existing = roleDOMapper.selectById(roleDO.getId());
            Long currentTenant = TenantContext.getTenantId();
            if (existing != null && "TENANT".equals(existing.getScope()) && currentTenant != null
                    && !currentTenant.equals(existing.getTenantId())) {
                throw new RuntimeException("无权修改其他租户的角色");
            }
        }
        roleDO.setUpdatedAt(new Date());
        roleDO.setUpdatedBy(operator);
        roleDOMapper.updateById(roleDO);

        // 更新角色权限关联
        if (role.getPermissionIds() != null) {
            updateRolePermissions(role.getId(), role.getPermissionIds(), operator);
        }

        return role;
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        // 检查是否为系统内置角色
        RoleDO roleDO = roleDOMapper.selectById(id);
        if (roleDO != null && roleDO.getIsSystem() != null && roleDO.getIsSystem() == 1) {
            throw new RuntimeException("系统内置角色不能删除");
        }
        // Guard: prevent cross-tenant deletion of TENANT-scope roles.
        Long currentTenant = TenantContext.getTenantId();
        if (roleDO != null && "TENANT".equals(roleDO.getScope()) && currentTenant != null
                && !currentTenant.equals(roleDO.getTenantId())) {
            throw new RuntimeException("无权删除其他租户的角色");
        }

        // 删除角色权限关联
        rolePermissionDOMapper.deleteByRoleId(id);
        // 删除角色
        roleDOMapper.deleteById(id);
    }

    @Override
    public List<String> getPermissionCodesByRoleId(Long roleId) {
        List<PermissionDO> permissions = permissionDOMapper.selectByRoleId(roleId);
        return permissions.stream().map(PermissionDO::getCode).collect(Collectors.toList());
    }

    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        List<PermissionDO> permissions = permissionDOMapper.selectByRoleId(roleId);
        return permissions.stream().map(PermissionDO::getId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateRolePermissions(Long roleId, List<Long> permissionIds, String operator) {
        // 先删除原有关联
        rolePermissionDOMapper.deleteByRoleId(roleId);

        // 插入新关联
        if (!CollectionUtils.isEmpty(permissionIds)) {
            List<RolePermissionDO> rolePermissions = permissionIds.stream().map(permissionId -> {
                RolePermissionDO rp = new RolePermissionDO();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                rp.setCreatedAt(new Date());
                return rp;
            }).collect(Collectors.toList());
            rolePermissionDOMapper.batchInsert(rolePermissions);
        }
    }

    private Role convertToRole(RoleDO roleDO) {
        Role role = new Role();
        role.setId(roleDO.getId());
        role.setName(roleDO.getName());
        role.setCode(roleDO.getCode());
        role.setDescription(roleDO.getDescription());
        role.setScope(roleDO.getScope());
        role.setTenantId(roleDO.getTenantId());
        role.setIsSystem(roleDO.getIsSystem() != null && roleDO.getIsSystem() == 1);
        role.setStatus(roleDO.getStatus() != null && roleDO.getStatus() == 1);
        role.setCreatedAt(roleDO.getCreatedAt());
        role.setCreatedBy(roleDO.getCreatedBy());
        role.setUpdatedAt(roleDO.getUpdatedAt());
        role.setUpdatedBy(roleDO.getUpdatedBy());
        return role;
    }

    private RoleDO convertToRoleDO(Role role) {
        RoleDO roleDO = new RoleDO();
        roleDO.setId(role.getId());
        roleDO.setName(role.getName());
        roleDO.setCode(role.getCode());
        roleDO.setDescription(role.getDescription());
        roleDO.setScope(role.getScope());
        roleDO.setTenantId(role.getTenantId());
        roleDO.setIsSystem(role.getIsSystem() != null && role.getIsSystem() ? 1 : 0);
        roleDO.setStatus(role.getStatus() == null || role.getStatus() ? 1 : 0);
        return roleDO;
    }
}
