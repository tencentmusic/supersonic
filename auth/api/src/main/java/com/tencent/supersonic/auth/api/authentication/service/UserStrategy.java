package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserStrategy {

    String getStrategyName();

    boolean accept(boolean isEnableAuthentication);

    User findUser(HttpServletRequest request, HttpServletResponse response);

    User findUser(String token, String appKey);
}
