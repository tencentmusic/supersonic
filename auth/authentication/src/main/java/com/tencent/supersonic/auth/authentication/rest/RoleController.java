package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.Role;
import com.tencent.supersonic.auth.api.authentication.service.RoleService;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/role")
@Slf4j
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;

    public RoleController(RoleService roleService, UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public List<Role> getRoleList(
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return roleService.getRoleList(tenantId);
    }

    @GetMapping("/scope/{scope}")
    public List<Role> getRolesByScope(@PathVariable String scope,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return roleService.getRolesByScope(scope, tenantId);
    }

    @GetMapping("/{id}")
    public Role getRoleById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        return roleService.getRoleById(id);
    }

    @GetMapping("/code/{code}")
    public Role getRoleByCode(@PathVariable String code,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return roleService.getRoleByCode(code, tenantId);
    }

    @PostMapping
    public Role createRole(@RequestBody Role role, HttpServletRequest request,
            HttpServletResponse response) throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        return roleService.createRole(role, user.getName());
    }

    @PutMapping
    public Role updateRole(@RequestBody Role role, HttpServletRequest request,
            HttpServletResponse response) throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        return roleService.updateRole(role, user.getName());
    }

    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        roleService.deleteRole(id);
    }

    @GetMapping("/{roleId}/permissions")
    public List<String> getPermissionCodesByRoleId(@PathVariable Long roleId,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        return roleService.getPermissionCodesByRoleId(roleId);
    }

    @GetMapping("/{roleId}/permission-ids")
    public List<Long> getPermissionIdsByRoleId(@PathVariable Long roleId,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        return roleService.getPermissionIdsByRoleId(roleId);
    }

    @PutMapping("/{roleId}/permissions")
    public void updateRolePermissions(@PathVariable Long roleId,
            @RequestBody List<Long> permissionIds, HttpServletRequest request,
            HttpServletResponse response) throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        roleService.updateRolePermissions(roleId, permissionIds, user.getName());
    }

    private void checkAdminPermission(User user) throws IllegalAccessException {
        if (user == null || user.getIsAdmin() != 1) {
            throw new IllegalAccessException("只有管理员才能执行此操作");
        }
    }
}
