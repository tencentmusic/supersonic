package com.tencent.supersonic.auth.authentication.adaptor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.adaptor.UserAdaptor;
import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.utils.UserTokenUtils;
import com.tencent.supersonic.common.util.ContextUtils;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DefaultUserAdaptor provides a default method to obtain user and organization information
 */
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
        Organization superSonic = new Organization("1", "0",
                "SuperSonic", "SuperSonic", Lists.newArrayList(), true);
        Organization hr = new Organization("2", "1",
                "Hr", "SuperSonic/Hr", Lists.newArrayList(), false);
        Organization sales = new Organization("3", "1",
                "Sales", "SuperSonic/Sales", Lists.newArrayList(), false);
        Organization marketing = new Organization("4", "1",
                "Marketing", "SuperSonic/Marketing", Lists.newArrayList(), false);
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
        userRepository.addUser(userDO);
    }

    @Override
    public String login(UserReq userReq) {
        UserTokenUtils userTokenUtils = ContextUtils.getBean(UserTokenUtils.class);
        UserDO userDO = getUser(userReq.getName());
        if (userDO == null) {
            throw new RuntimeException("user not exist,please register");
        }
        if (userDO.getPassword().equals(userReq.getPassword())) {
            UserWithPassword user = UserWithPassword.get(userDO.getId(), userDO.getName(), userDO.getDisplayName(),
                    userDO.getEmail(), userDO.getPassword(), userDO.getIsAdmin());
            return userTokenUtils.generateToken(user);
        }
        throw new RuntimeException("password not correct, please try again");
    }

    @Override
    public List<User> getUserByOrg(String key) {
        return Lists.newArrayList();
    }

    @Override
    public Set<String> getUserAllOrgId(String userName) {
        return Sets.newHashSet();
    }

}