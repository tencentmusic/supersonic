package com.tencent.supersonic.auth.authentication.strategy;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.auth.authentication.utils.UserTokenUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;


@Service
public class HttpHeaderUserStrategy implements UserStrategy {


    private final UserTokenUtils userTokenUtils;


    public HttpHeaderUserStrategy(UserTokenUtils userTokenUtils) {
        this.userTokenUtils = userTokenUtils;
    }

    @Override
    public boolean accept(boolean isEnableAuthentication) {
        return isEnableAuthentication;
    }

    @Override
    public User findUser(HttpServletRequest request, HttpServletResponse response) {
        return userTokenUtils.getUser(request);
    }

    @Override
    public User findUser(String token, String appKey) {
        return userTokenUtils.getUser(token, appKey);
    }
}
