package com.tencent.supersonic.auth.authentication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.pojo.Permission;
import com.tencent.supersonic.auth.api.authentication.service.PermissionService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.PermissionDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.PermissionDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionDOMapper permissionDOMapper;
    private final UserDOMapper userDOMapper;

    public PermissionServiceImpl(PermissionDOMapper permissionDOMapper, UserDOMapper userDOMapper) {
        this.permissionDOMapper = permissionDOMapper;
        this.userDOMapper = userDOMapper;
    }

    @Override
    public List<Permission> getAllPermissions() {
        LambdaQueryWrapper<PermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PermissionDO::getStatus, 1).orderByAsc(PermissionDO::getSortOrder);
        List<PermissionDO> permissionDOList = permissionDOMapper.selectList(wrapper);
        return permissionDOList.stream().map(this::convertToPermission)
                .collect(Collectors.toList());
    }

    @Override
    public List<Permission> getPermissionsByType(String type) {
        List<PermissionDO> permissionDOList = permissionDOMapper.selectByType(type);
        return permissionDOList.stream().map(this::convertToPermission)
                .collect(Collectors.toList());
    }

    @Override
    public List<Permission> getPermissionsByScope(String scope) {
        LambdaQueryWrapper<PermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PermissionDO::getScope, scope).eq(PermissionDO::getStatus, 1)
                .orderByAsc(PermissionDO::getSortOrder);
        List<PermissionDO> permissionDOList = permissionDOMapper.selectList(wrapper);
        return permissionDOList.stream().map(this::convertToPermission)
                .collect(Collectors.toList());
    }

    @Override
    public Permission getPermissionById(Long id) {
        PermissionDO permissionDO = permissionDOMapper.selectById(id);
        return permissionDO == null ? null : convertToPermission(permissionDO);
    }

    @Override
    public Permission getPermissionByCode(String code) {
        PermissionDO permissionDO = permissionDOMapper.selectByCode(code);
        return permissionDO == null ? null : convertToPermission(permissionDO);
    }

    @Override
    @Transactional
    public Permission createPermission(Permission permission) {
        PermissionDO permissionDO = convertToPermissionDO(permission);
        permissionDO.setCreatedAt(new Date());
        permissionDO.setUpdatedAt(new Date());
        permissionDOMapper.insert(permissionDO);
        permission.setId(permissionDO.getId());
        return permission;
    }

    @Override
    @Transactional
    public Permission updatePermission(Permission permission) {
        PermissionDO permissionDO = convertToPermissionDO(permission);
        permissionDO.setUpdatedAt(new Date());
        permissionDOMapper.updateById(permissionDO);
        return permission;
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        // 软删除
        PermissionDO permissionDO = permissionDOMapper.selectById(id);
        if (permissionDO != null) {
            permissionDO.setStatus(0);
            permissionDO.setUpdatedAt(new Date());
            permissionDOMapper.updateById(permissionDO);
        }
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        UserDO userDO = userDOMapper.selectById(userId);
        if (userDO == null) {
            return Collections.emptyList();
        }

        // 如果是管理员，返回所有权限
        if (userDO.getIsAdmin() != null && userDO.getIsAdmin() == 1) {
            return getAllPermissions().stream().map(Permission::getCode)
                    .collect(Collectors.toList());
        }

        // Non-admin users: resolve permissions via user-role-role_permission mapping.
        List<PermissionDO> permissions = permissionDOMapper.selectByUserId(userId);
        return permissions.stream().map(PermissionDO::getCode).collect(Collectors.toList());
    }

    @Override
    public List<Permission> getPermissionTree() {
        List<Permission> allPermissions = getAllPermissions();
        return buildTree(allPermissions, null);
    }

    private List<Permission> buildTree(List<Permission> permissions, Long parentId) {
        List<Permission> result = new ArrayList<>();
        for (Permission permission : permissions) {
            Long pId = permission.getParentId();
            boolean isChild =
                    (parentId == null && pId == null) || (parentId != null && parentId.equals(pId));
            if (isChild) {
                List<Permission> children = buildTree(permissions, permission.getId());
                permission.setChildren(children.isEmpty() ? null : children);
                result.add(permission);
            }
        }
        return result;
    }

    private Permission convertToPermission(PermissionDO permissionDO) {
        Permission permission = new Permission();
        permission.setId(permissionDO.getId());
        permission.setName(permissionDO.getName());
        permission.setCode(permissionDO.getCode());
        permission.setType(permissionDO.getType());
        permission.setScope(permissionDO.getScope());
        permission.setParentId(permissionDO.getParentId());
        permission.setPath(permissionDO.getPath());
        permission.setIcon(permissionDO.getIcon());
        permission.setSortOrder(permissionDO.getSortOrder());
        permission.setDescription(permissionDO.getDescription());
        permission.setStatus(permissionDO.getStatus() != null && permissionDO.getStatus() == 1);
        permission.setCreatedAt(permissionDO.getCreatedAt());
        permission.setUpdatedAt(permissionDO.getUpdatedAt());
        return permission;
    }

    private PermissionDO convertToPermissionDO(Permission permission) {
        PermissionDO permissionDO = new PermissionDO();
        permissionDO.setId(permission.getId());
        permissionDO.setName(permission.getName());
        permissionDO.setCode(permission.getCode());
        permissionDO.setType(permission.getType());
        permissionDO.setScope(permission.getScope());
        permissionDO.setParentId(permission.getParentId());
        permissionDO.setPath(permission.getPath());
        permissionDO.setIcon(permission.getIcon());
        permissionDO.setSortOrder(permission.getSortOrder());
        permissionDO.setDescription(permission.getDescription());
        permissionDO.setStatus(permission.getStatus() == null || permission.getStatus() ? 1 : 0);
        return permissionDO;
    }
}
