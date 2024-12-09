package com.tencent.supersonic.auth.authentication.adaptor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.adaptor.UserAdaptor;
import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.UserToken;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserTokenDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.AESEncryptionUtil;
import com.tencent.supersonic.common.util.ContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.List;
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
        Organization superSonic =
                new Organization("1", "0", "SuperSonic", "SuperSonic", Lists.newArrayList(), true);
        Organization hr =
                new Organization("2", "1", "Hr", "SuperSonic/Hr", Lists.newArrayList(), false);
        Organization sales = new Organization("3", "1", "Sales", "SuperSonic/Sales",
                Lists.newArrayList(), false);
        Organization marketing = new Organization("4", "1", "Marketing", "SuperSonic/Marketing",
                Lists.newArrayList(), false);
        List<Organization> subOrganization = Lists.newArrayList(hr, sales, marketing);
        superSonic.setSubOrganizations(subOrganization);
        return Lists.newArrayList(superSonic);
    }

    private User convert(UserDO userDO) {
        User user = new User();
        BeanUtils.copyProperties(userDO, user);
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
        userRepository.addUser(userDO);
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
            return tokenService.generateToken(UserWithPassword.convert(user), appKey);
        } catch (Exception e) {
            log.error("", e);
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
                UserWithPassword user = UserWithPassword.get(userDO.getId(), userDO.getName(),
                        userDO.getDisplayName(), userDO.getEmail(), userDO.getPassword(),
                        userDO.getIsAdmin());
                return user;
            } else {
                throw new RuntimeException("password not correct, please try again");
            }
        } catch (Exception e) {
            throw new RuntimeException("password encrypt error, please try again");
        }
    }

    @Override
    public List<User> getUserByOrg(String key) {
        return Lists.newArrayList();
    }

    @Override
    public Set<String> getUserAllOrgId(String userName) {
        return Sets.newHashSet();
    }

    @Override
    public UserToken generateToken(String name, String userName, long expireTime) {
        TokenService tokenService = ContextUtils.getBean(TokenService.class);
        UserDO userDO = getUser(userName);
        if (userDO == null) {
            throw new RuntimeException("user not exist,please register");
        }
        UserWithPassword userWithPassword =
                new UserWithPassword(userDO.getId(), userDO.getName(), userDO.getDisplayName(),
                        userDO.getEmail(), userDO.getPassword(), userDO.getIsAdmin());

        String token =
                tokenService.generateToken(UserWithPassword.convert(userWithPassword), expireTime);
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
        List<UserToken> userTokens = userRepository.getUserTokenListByName(userName).stream()
                .map(this::convertUserToken).collect(Collectors.toList());
        return userTokens;
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
}
