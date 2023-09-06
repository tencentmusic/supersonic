package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;

import java.util.List;
import java.util.Set;

public interface UserService {

    List<String> getUserNames();

    List<User> getUserList();

    void register(UserReq userCmd);

    String login(UserReq userCmd);

    Set<String> getUserAllOrgId(String userName);

    List<User> getUserByOrg(String key);

    List<Organization> getOrganizationTree();
}
