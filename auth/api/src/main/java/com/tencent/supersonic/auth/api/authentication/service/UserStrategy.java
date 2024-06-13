package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface UserStrategy {

    boolean accept(boolean isEnableAuthentication);

    User findUser(HttpServletRequest request, HttpServletResponse response);

    User findUser(String token, String appKey);

}
