package com.tencent.supersonic.auth.api.authentication.adaptor;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;

import java.util.List;
import java.util.Set;

public interface UserAdaptor {

    List<String> getUserNames();

    List<User> getUserList();

    List<Organization> getOrganizationTree();

    void register(UserReq userReq);

    String login(UserReq userReq);

    List<User> getUserByOrg(String key);

    Set<String> getUserAllOrgId(String userName);
}
