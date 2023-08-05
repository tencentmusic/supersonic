package com.tencent.supersonic.auth.authentication.persistence.repository;

import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import java.util.List;

public interface UserRepository {

    List<UserDO> getUserList();

    void addUser(UserDO userDO);

    UserDO getUser(String name);
}
