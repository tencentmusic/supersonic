package com.tencent.supersonic.auth.authentication.domain.repository;

import com.tencent.supersonic.auth.authentication.domain.dataobject.UserDO;
import java.util.List;

public interface UserRepository {

    List<UserDO> getUserList();

    void addUser(UserDO userDO);

    UserDO getUser(String name);
}
