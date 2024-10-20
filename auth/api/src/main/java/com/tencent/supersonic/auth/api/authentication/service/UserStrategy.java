package com.tencent.supersonic.auth.api.authentication.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.common.pojo.User;

public interface UserStrategy {

    boolean accept(boolean isEnableAuthentication);

    User findUser(HttpServletRequest request, HttpServletResponse response);

    User findUser(String token, String appKey);
}
