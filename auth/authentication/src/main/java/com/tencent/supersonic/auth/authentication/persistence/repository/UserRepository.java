package com.tencent.supersonic.auth.authentication.persistence.repository;

import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserTokenDO;

import java.util.List;

public interface UserRepository {

    List<UserDO> getUserList();

    void addUser(UserDO userDO);

    void updateUser(UserDO userDO);

    UserDO getUser(String name);

    List<UserTokenDO> getUserTokenListByName(String userName);

    void addUserToken(UserTokenDO userTokenDO);

    UserTokenDO getUserToken(Long tokenId);

    void deleteUserTokenByName(String userName);

    void deleteUserToken(Long tokenId);
}
