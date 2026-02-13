package com.tencent.supersonic.auth.authentication.utils;

import com.tencent.supersonic.auth.api.authentication.service.InternalTokenGenerator;
import com.tencent.supersonic.common.pojo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_IS_ADMIN;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_TENANT_ID;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_DISPLAY_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_EMAIL;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_ID;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_NAME;

@Component
@RequiredArgsConstructor
public class InternalTokenGeneratorImpl implements InternalTokenGenerator {

    private final TokenService tokenService;

    @Override
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_USER_ID, user.getId());
        claims.put(TOKEN_USER_NAME, user.getName());
        claims.put(TOKEN_USER_DISPLAY_NAME,
                user.getDisplayName() != null ? user.getDisplayName() : user.getName());
        claims.put(TOKEN_USER_EMAIL, user.getEmail());
        claims.put(TOKEN_IS_ADMIN, user.getIsAdmin() != null ? user.getIsAdmin() : 0);
        if (user.getTenantId() != null) {
            claims.put(TOKEN_TENANT_ID, user.getTenantId());
        }
        return tokenService.generateToken(claims, tokenService.getDefaultAppKey());
    }
}
