package com.tencent.supersonic.auth.authentication.strategy;

import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class FakeUserStrategy implements UserStrategy {

    public static final String STRATEGY_NAME = "fake";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

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
