package com.tencent.supersonic.auth.api.authentication.adaptor;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;

import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * UserAdaptor defines some interfaces for obtaining user and organization information
 */
public interface UserAdaptor {

    List<String> getUserNames();

    List<String> getUserPermissions(String userName);

    List<User> getUserList();

    List<Organization> getOrganizationTree();

    void register(UserReq userReq);

    String login(UserReq userReq, HttpServletRequest request);

    String login(UserReq userReq, String appKey);

    List<User> getUserByOrg(String key);

    Set<String> getUserAllOrgId(String userName);

    UserWithPassword getUserByName(String name);

    String casLogin(String prefixUrl, String ticket, String service, String appKey);
}
