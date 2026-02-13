package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.common.pojo.User;

/**
 * Generates JWT tokens for internal service-to-service calls. This interface lives in auth-api so
 * that modules like feishu-server can generate tokens without depending on auth-authentication.
 */
public interface InternalTokenGenerator {

    /**
     * Generate a JWT token representing the given user. The token can be used in HTTP Authorization
     * header for internal API calls.
     *
     * @param user the user to generate a token for
     * @return JWT token string (without "Bearer " prefix)
     */
    String generateToken(User user);
}
