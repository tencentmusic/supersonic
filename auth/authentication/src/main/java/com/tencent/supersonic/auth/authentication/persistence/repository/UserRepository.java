package com.tencent.supersonic.auth.authentication.persistence.repository;

import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserTokenDO;

import java.util.List;

public interface UserRepository {

    List<UserDO> getUserList();

    void addUser(UserDO userDO);

    List<UserTokenDO> getUserTokenListByName(String userName);

    UserDO getUser(String name);

    void updateUser(UserDO userDO);

    void addUserToken(UserTokenDO userTokenDO);

    UserTokenDO getUserToken(Long tokenId);

    void deleteUserTokenByName(String userName);

    void deleteUserToken(Long tokenId);
}
