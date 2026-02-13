package com.tencent.supersonic.auth.api.authentication.service;



import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.UserToken;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Set;

public interface UserService {

    User getCurrentUser(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse);

    List<String> getUserNames();

    List<User> getUserList();

    void register(UserReq userCmd);

    void deleteUser(long userId);

    String login(UserReq userCmd, HttpServletRequest request);

    String login(UserReq userCmd, String appKey);

    Set<String> getUserAllOrgId(String userName);

    List<User> getUserByOrg(String key);

    List<Organization> getOrganizationTree();

    String getPassword(String userName);

    void resetPassword(String userName, String password, String newPassword);

    UserToken generateToken(String name, String userName, long expireTime);

    List<UserToken> getUserTokens(String userName);

    UserToken getUserToken(Long id);

    void deleteUserToken(Long id);

    /**
     * Assign roles to a user (replace all existing roles)
     */
    void assignRolesToUser(Long userId, List<Long> roleIds, String operator);

    /**
     * Get role IDs for a user
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * Get a user by ID.
     */
    User getUserById(Long userId);

    /**
     * Get the system admin user (queried from database).
     */
    User getDefaultUser();

    /**
     * Get the anonymous visitor user (fallback when authentication fails).
     */
    User getVisitUser();

    /**
     * Get the application user (for mobile/app scenarios).
     */
    User getAppUser(int appId);
}
