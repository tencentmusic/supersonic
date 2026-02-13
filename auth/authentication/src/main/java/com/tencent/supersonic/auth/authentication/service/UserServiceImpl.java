package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.Permission;
import com.tencent.supersonic.auth.api.authentication.pojo.UserToken;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.service.PermissionService;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserRoleDOMapper;
import com.tencent.supersonic.auth.authentication.utils.ComponentFactory;
import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final SystemConfigService sysParameterService;
    private final PermissionService permissionService;
    private final UserRoleDOMapper userRoleDOMapper;

    public UserServiceImpl(SystemConfigService sysParameterService,
            PermissionService permissionService, UserRoleDOMapper userRoleDOMapper) {
        this.sysParameterService = sysParameterService;
        this.permissionService = permissionService;
        this.userRoleDOMapper = userRoleDOMapper;
    }

    @Override
    public User getCurrentUser(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        if (user != null) {
            SystemConfig systemConfig = sysParameterService.getSystemConfig();
            if (!CollectionUtils.isEmpty(systemConfig.getAdmins())
                    && systemConfig.getAdmins().contains(user.getName())) {
                user.setIsAdmin(1);
            }
            // 填充用户权限
            fillUserPermissions(user);
        }
        return user;
    }

    /**
     * 填充用户权限
     */
    private void fillUserPermissions(User user) {
        List<String> permissions;
        // 管理员拥有所有权限
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            permissions = permissionService.getAllPermissions().stream().map(Permission::getCode)
                    .collect(Collectors.toList());
        } else if (user.getId() != null) {
            permissions = permissionService.getPermissionCodesByUserId(user.getId());
        } else {
            permissions = List.of();
        }
        user.setPermissions(permissions);
    }

    @Override
    public List<String> getUserNames() {
        return ComponentFactory.getUserAdaptor().getUserNames();
    }

    @Override
    public List<User> getUserList() {
        return ComponentFactory.getUserAdaptor().getUserList();
    }

    @Override
    public Set<String> getUserAllOrgId(String userName) {
        return ComponentFactory.getUserAdaptor().getUserAllOrgId(userName);
    }

    @Override
    public List<User> getUserByOrg(String key) {
        return ComponentFactory.getUserAdaptor().getUserByOrg(key);
    }

    @Override
    public List<Organization> getOrganizationTree() {
        return ComponentFactory.getUserAdaptor().getOrganizationTree();
    }

    @Override
    public void register(UserReq userReq) {
        ComponentFactory.getUserAdaptor().register(userReq);
    }

    @Override
    public void deleteUser(long userId) {
        ComponentFactory.getUserAdaptor().deleteUser(userId);
    }

    @Override
    public String login(UserReq userReq, HttpServletRequest request) {
        return ComponentFactory.getUserAdaptor().login(userReq, request);
    }

    @Override
    public String login(UserReq userReq, String appKey) {
        return ComponentFactory.getUserAdaptor().login(userReq, appKey);
    }

    @Override
    public String getPassword(String userName) {
        return ComponentFactory.getUserAdaptor().getPassword(userName);
    }

    @Override
    public void resetPassword(String userName, String password, String newPassword) {
        ComponentFactory.getUserAdaptor().resetPassword(userName, password, newPassword);
    }

    @Override
    public UserToken generateToken(String name, String userName, long expireTime) {
        return ComponentFactory.getUserAdaptor().generateToken(name, userName, expireTime);
    }

    @Override
    public List<UserToken> getUserTokens(String userName) {
        return ComponentFactory.getUserAdaptor().getUserTokens(userName);
    }

    @Override
    public UserToken getUserToken(Long id) {
        return ComponentFactory.getUserAdaptor().getUserToken(id);
    }

    @Override
    public void deleteUserToken(Long id) {
        ComponentFactory.getUserAdaptor().deleteUserToken(id);
    }

    @Override
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds, String operator) {
        // 只删除用户的租户级角色，保留平台级角色
        // 这样租户管理员分配角色时不会影响用户的平台级角色
        userRoleDOMapper.deleteTenantRolesByUserId(userId);
        // Insert new role assignments
        if (!CollectionUtils.isEmpty(roleIds)) {
            userRoleDOMapper.batchInsert(userId, roleIds, operator);
        }
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userRoleDOMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public User getUserById(Long userId) {
        return ComponentFactory.getUserAdaptor().getUserById(userId);
    }

    @Override
    public User getDefaultUser() {
        try {
            User user = ComponentFactory.getUserAdaptor().getUserByName("admin");
            if (user != null) {
                return user;
            }
        } catch (Exception e) {
            // Fallback to hardcoded value when database is not ready (e.g. during startup)
        }
        return User.getDefaultUser();
    }

    @Override
    public User getVisitUser() {
        try {
            User user = ComponentFactory.getUserAdaptor().getUserByName("visit");
            if (user != null) {
                return user;
            }
        } catch (Exception e) {
            // Fallback to hardcoded value when database is not ready (e.g. during startup)
        }
        return User.getVisitUser();
    }

    @Override
    public User getAppUser(int appId) {
        return User.getAppUser(appId);
    }
}
