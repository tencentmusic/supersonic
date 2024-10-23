package com.tencent.supersonic.auth.authentication.strategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.common.pojo.User;
import org.springframework.stereotype.Service;

@Service
public class FakeUserStrategy implements UserStrategy {

    @Override
    public boolean accept(boolean isEnableAuthentication) {
        return !isEnableAuthentication;
    }

    @Override
    public User findUser(HttpServletRequest request, HttpServletResponse response) {
        return User.getDefaultUser();
    }

    @Override
    public User findUser(String token, String appKey) {
        return User.getDefaultUser();
    }
}
