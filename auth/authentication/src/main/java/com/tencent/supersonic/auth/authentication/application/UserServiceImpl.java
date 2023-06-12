package com.tencent.supersonic.auth.authentication.application;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.authentication.domain.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.domain.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.domain.utils.UserTokenUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    private UserTokenUtils userTokenUtils;

    public UserServiceImpl(UserRepository userRepository, UserTokenUtils userTokenUtils) {
        this.userRepository = userRepository;
        this.userTokenUtils = userTokenUtils;
    }


    private List<UserDO> getUserDOList() {
        return userRepository.getUserList();
    }

    private UserDO getUser(String name) {
        return userRepository.getUser(name);
    }

    public boolean checkExist(UserWithPassword user) {
        UserDO userDO = getUser(user.getName());
        if (userDO == null) {
            return false;
        }
        return userDO.getPassword().equals(user.getPassword());
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

    private User convert(UserDO userDO) {
        User user = new User();
        BeanUtils.copyProperties(userDO, user);
        return user;
    }


    @Override
    public void register(UserReq userReq) {
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
        UserDO userDO = getUser(userReq.getName());
        if (userDO == null) {
            throw new RuntimeException("user not exist,please register");
        }
        if (userDO.getPassword().equals(userReq.getPassword())) {
            UserWithPassword user = UserWithPassword.get(userDO.getId(), userDO.getName(), userDO.getDisplayName(),
                    userDO.getEmail(), userDO.getPassword());
            return userTokenUtils.generateToken(user);
        }
        throw new RuntimeException("password not correct, please try again");
    }


}
