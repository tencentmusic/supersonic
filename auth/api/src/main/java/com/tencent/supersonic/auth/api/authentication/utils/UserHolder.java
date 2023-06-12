package com.tencent.supersonic.auth.api.authentication.utils;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class UserHolder {

    private static UserStrategy REPO;

    public static synchronized void setStrategy(UserStrategy strategy) {
        REPO = strategy;
    }

    public static User findUser(HttpServletRequest request, HttpServletResponse response) {
        return REPO.findUser(request, response);
    }

}
