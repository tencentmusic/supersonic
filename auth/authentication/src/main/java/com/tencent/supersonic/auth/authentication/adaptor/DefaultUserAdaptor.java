package com.tencent.supersonic.auth.authentication.adaptor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.adaptor.UserAdaptor;
import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.UserToken;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OrganizationDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserOrganizationDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserTokenDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserRoleDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.repository.OrganizationRepository;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.AESEncryptionUtil;
import com.tencent.supersonic.common.util.ContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DefaultUserAdaptor provides a default method to obtain user and organization information
 */
@Slf4j
public class DefaultUserAdaptor implements UserAdaptor {

    private List<UserDO> getUserDOList() {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        return userRepository.getUserList();
    }

    private UserDO getUser(String name) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        return userRepository.getUser(name);
    }

    @Override
    public List<String> getUserNames() {
        return getUserDOList().stream().map(UserDO::getName).collect(Collectors.toList());
    }

    @Override
    public List<User> getUserList() {
        List<UserDO> userDOS = getUserDOList();
        return userDOS.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public List<Organization> getOrganizationTree() {
        OrganizationRepository organizationRepository =
                ContextUtils.getBean(OrganizationRepository.class);
        Long tenantId = TenantContext.getTenantIdOrDefault(1L);

        // 获取当前租户的所有组织
        List<OrganizationDO> allOrganizations =
                organizationRepository.getOrganizationListByTenantId(tenantId);

        if (allOrganizations.isEmpty()) {
            // 如果数据库中没有组织数据，返回空列表
            return Lists.newArrayList();
        }

        // 构建组织树
        return buildOrganizationTree(allOrganizations);
    }

    /**
     * 将扁平的组织列表构建成树形结构
     */
    private List<Organization> buildOrganizationTree(List<OrganizationDO> allOrganizations) {
        // 转换为 Organization 对象的 Map，方便查找
        Map<Long, Organization> orgMap = new HashMap<>();
        for (OrganizationDO orgDO : allOrganizations) {
            Organization org = convertToOrganization(orgDO);
            orgMap.put(orgDO.getId(), org);
        }

        // 构建树形结构
        List<Organization> roots = new ArrayList<>();
        for (OrganizationDO orgDO : allOrganizations) {
            Organization org = orgMap.get(orgDO.getId());
            if (orgDO.getIsRoot() != null && orgDO.getIsRoot() == 1) {
                // 根组织
                roots.add(org);
            } else if (orgDO.getParentId() != null && orgDO.getParentId() > 0) {
                // 非根组织，添加到父组织的子列表中
                Organization parent = orgMap.get(orgDO.getParentId());
                if (parent != null) {
                    parent.getSubOrganizations().add(org);
                }
            }
        }

        return roots;
    }

    /**
     * 将 OrganizationDO 转换为 Organization
     */
    private Organization convertToOrganization(OrganizationDO orgDO) {
        Organization org = new Organization();
        org.setId(String.valueOf(orgDO.getId()));
        org.setParentId(orgDO.getParentId() != null ? String.valueOf(orgDO.getParentId()) : "0");
        org.setName(orgDO.getName());
        org.setFullName(orgDO.getFullName());
        org.setRoot(orgDO.getIsRoot() != null && orgDO.getIsRoot() == 1);
        org.setSortOrder(orgDO.getSortOrder());
        org.setStatus(orgDO.getStatus());
        return org;
    }

    @Override
    public User getUserByName(String name) {
        UserDO userDO = getUser(name);
        if (userDO == null) {
            return null;
        }
        return convert(userDO);
    }

    private User convert(UserDO userDO) {
        User user = new User();
        BeanUtils.copyProperties(userDO, user);

        // Populate organization information
        try {
            OrganizationRepository organizationRepository =
                    ContextUtils.getBean(OrganizationRepository.class);
            List<UserOrganizationDO> userOrgs =
                    organizationRepository.getUserOrganizations(userDO.getId());
            if (userOrgs != null && !userOrgs.isEmpty()) {
                // Find primary organization first, or use the first one
                UserOrganizationDO primaryOrg = userOrgs.stream()
                        .filter(uo -> uo.getIsPrimary() != null && uo.getIsPrimary() == 1)
                        .findFirst().orElse(userOrgs.get(0));
                user.setOrganizationId(primaryOrg.getOrganizationId());

                // Get organization name
                OrganizationDO orgDO =
                        organizationRepository.getOrganization(primaryOrg.getOrganizationId());
                if (orgDO != null) {
                    user.setOrganizationName(orgDO.getName());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load organization info for user {}: {}", userDO.getName(),
                    e.getMessage());
        }

        // Populate role information
        try {
            UserRoleDOMapper userRoleDOMapper = ContextUtils.getBean(UserRoleDOMapper.class);
            List<Long> roleIds = userRoleDOMapper.selectRoleIdsByUserId(userDO.getId());
            List<String> roleNames = userRoleDOMapper.selectRoleNamesByUserId(userDO.getId());
            user.setRoleIds(roleIds);
            user.setRoleNames(roleNames);
        } catch (Exception e) {
            log.warn("Failed to load role info for user {}: {}", userDO.getName(), e.getMessage());
        }

        return user;
    }

    @Override
    public void register(UserReq userReq) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        List<String> userDOS = getUserNames();
        if (userDOS.contains(userReq.getName())) {
            throw new RuntimeException(String.format("user %s exist", userReq.getName()));
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userReq, userDO);
        try {
            byte[] salt = AESEncryptionUtil.generateSalt(userDO.getName());
            userDO.setSalt(AESEncryptionUtil.getStringFromBytes(salt));
            userDO.setPassword(AESEncryptionUtil.encrypt(userReq.getPassword(), salt));
        } catch (Exception e) {
            throw new RuntimeException("password encrypt error, please try again");
        }
        // Set tenant ID from context or use default tenant (1)
        Long tenantId = TenantContext.getTenantIdOrDefault(1L);
        userDO.setTenantId(tenantId);
        userRepository.addUser(userDO);
    }

    @Override
    public void deleteUser(long userId) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        userRepository.deleteUser(userId);
    }

    @Override
    public String login(UserReq userReq, HttpServletRequest request) {
        TokenService tokenService = ContextUtils.getBean(TokenService.class);
        String appKey = tokenService.getAppKey(request);
        return login(userReq, appKey);
    }

    @Override
    public String login(UserReq userReq, String appKey) {
        TokenService tokenService = ContextUtils.getBean(TokenService.class);
        try {
            UserWithPassword user = getUserWithPassword(userReq);
            String token = tokenService.generateToken(UserWithPassword.convert(user), appKey);
            updateLastLogin(userReq.getName());
            return token;
        } catch (Exception e) {
            log.error("Login error", e);
            throw new RuntimeException("password encrypt error, please try again");
        }
    }

    @Override
    public String getPassword(String userName) {
        UserDO userDO = getUser(userName);
        if (userDO == null) {
            throw new RuntimeException("user not exist,please register");
        }
        return userDO.getPassword();
    }

    @Override
    public void resetPassword(String userName, String password, String newPassword) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        Optional<UserDO> userDOOptional = Optional.ofNullable(getUser(userName));

        UserDO userDO = userDOOptional
                .orElseThrow(() -> new RuntimeException("User does not exist, please register"));

        try {
            validateOldPassword(userDO, password);
            updatePassword(userDO, newPassword, userRepository);
        } catch (PasswordEncryptionException e) {
            throw new RuntimeException("Password encryption error, please try again", e);
        }
    }


    private void validateOldPassword(UserDO userDO, String password)
            throws PasswordEncryptionException {
        String oldPassword = encryptPassword(password, userDO.getSalt());
        if (!userDO.getPassword().equals(oldPassword)) {
            throw new RuntimeException("Old password is not correct, please try again");
        }
    }

    private void updatePassword(UserDO userDO, String newPassword, UserRepository userRepository)
            throws PasswordEncryptionException {
        try {
            byte[] salt = AESEncryptionUtil.generateSalt(userDO.getName());
            userDO.setSalt(AESEncryptionUtil.getStringFromBytes(salt));
            userDO.setPassword(AESEncryptionUtil.encrypt(newPassword, salt));
            userRepository.updateUser(userDO);
        } catch (Exception e) {
            throw new PasswordEncryptionException("Error encrypting password", e);
        }

    }

    private String encryptPassword(String password, String salt)
            throws PasswordEncryptionException {
        try {
            return AESEncryptionUtil.encrypt(password, AESEncryptionUtil.getBytesFromString(salt));
        } catch (Exception e) {
            throw new PasswordEncryptionException("Error encrypting password", e);
        }
    }

    public static class PasswordEncryptionException extends Exception {
        public PasswordEncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private UserWithPassword getUserWithPassword(UserReq userReq) {
        UserDO userDO = getUser(userReq.getName());
        if (userDO == null) {
            throw new RuntimeException("user not exist,please register");
        }
        try {
            String password = AESEncryptionUtil.encrypt(userReq.getPassword(),
                    AESEncryptionUtil.getBytesFromString(userDO.getSalt()));
            if (userDO.getPassword().equals(password)) {
                return UserWithPassword.get(userDO.getId(), userDO.getName(),
                        userDO.getDisplayName(), userDO.getEmail(), userDO.getPassword(),
                        userDO.getIsAdmin(), userDO.getTenantId(), null);
            } else {
                throw new RuntimeException("password not correct, please try again");
            }
        } catch (Exception e) {
            throw new RuntimeException("password encrypt error, please try again");
        }
    }

    @Override
    public List<User> getUserByOrg(String key) {
        OrganizationRepository organizationRepository =
                ContextUtils.getBean(OrganizationRepository.class);
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);

        try {
            Long orgId = Long.parseLong(key);
            List<Long> userIds = organizationRepository.getUserIdsByOrganizationId(orgId);
            if (userIds.isEmpty()) {
                return Lists.newArrayList();
            }
            return userIds.stream().map(userRepository::getUser).filter(userDO -> userDO != null)
                    .map(this::convert).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            // key 不是数字，可能是组织名称，暂不支持
            return Lists.newArrayList();
        }
    }

    @Override
    public Set<String> getUserAllOrgId(String userName) {
        OrganizationRepository organizationRepository =
                ContextUtils.getBean(OrganizationRepository.class);
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);

        UserDO userDO = userRepository.getUser(userName);
        if (userDO == null) {
            return Sets.newHashSet();
        }

        List<Long> orgIds = organizationRepository.getOrganizationIdsByUserId(userDO.getId());
        return orgIds.stream().map(String::valueOf).collect(Collectors.toSet());
    }

    @Override
    public UserToken generateToken(String name, String userName, long expireTime) {
        TokenService tokenService = ContextUtils.getBean(TokenService.class);
        UserDO userDO = getUser(userName);
        if (userDO == null) {
            throw new RuntimeException("user not exist,please register");
        }
        UserWithPassword userWithPassword = new UserWithPassword(userDO.getId(), userDO.getName(),
                userDO.getDisplayName(), userDO.getEmail(), userDO.getPassword(),
                userDO.getIsAdmin(), userDO.getTenantId(), null);

        // 使用令牌名称作为生成key ，这样可以区分正常请求和api 请求，api 的令牌失效时间很长，需考虑令牌泄露的情况
        String token = tokenService.generateToken(UserWithPassword.convert(userWithPassword),
                "SysDbToken:" + name, (new Date().getTime() + expireTime));
        UserTokenDO userTokenDO = saveUserToken(name, userName, token, expireTime);
        return convertUserToken(userTokenDO);
    }

    @Override
    public void deleteUserToken(Long id) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        userRepository.deleteUserToken(id);
    }

    @Override
    public UserToken getUserToken(Long id) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        return convertUserToken(userRepository.getUserToken(id));
    }

    @Override
    public List<UserToken> getUserTokens(String userName) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        return userRepository.getUserTokenListByName(userName).stream().map(this::convertUserToken)
                .collect(Collectors.toList());
    }

    private UserTokenDO saveUserToken(String tokenName, String userName, String token,
            long expireTime) {
        UserTokenDO userTokenDO = new UserTokenDO();
        userTokenDO.setName(tokenName);
        userTokenDO.setUserName(userName);
        userTokenDO.setToken(token);
        userTokenDO.setExpireTime(expireTime);
        userTokenDO.setCreateTime(new java.util.Date());
        userTokenDO.setCreateBy(userName);
        userTokenDO.setUpdateBy(userName);
        userTokenDO.setExpireDateTime(new java.util.Date(System.currentTimeMillis() + expireTime));
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        userRepository.addUserToken(userTokenDO);

        return userTokenDO;
    }

    private UserToken convertUserToken(UserTokenDO userTokenDO) {
        UserToken userToken = new UserToken();
        userToken.setId(userTokenDO.getId());
        userToken.setName(userTokenDO.getName());
        userToken.setUserName(userTokenDO.getUserName());
        userToken.setToken(userTokenDO.getToken());
        userToken.setExpireTime(userTokenDO.getExpireTime());
        userToken.setCreateDate(userTokenDO.getCreateTime());
        userToken.setExpireDate(userTokenDO.getExpireDateTime());
        return userToken;
    }

    private void updateLastLogin(String userName) {
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        UserDO userDO = userRepository.getUser(userName);
        userDO.setLastLogin(new Timestamp(System.currentTimeMillis()));
        userRepository.updateUser(userDO);
    }
}
