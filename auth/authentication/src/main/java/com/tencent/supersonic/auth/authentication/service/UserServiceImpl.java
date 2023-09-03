package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.authentication.utils.ComponentFactory;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {


    @Override
    public List<String> getUserNames() {
        return ComponentFactory.getUserAdaptor().getUserNames();
    }

    @Override
    public List<User> getUserList() {
        return ComponentFactory.getUserAdaptor().getUserList();
    }

    @Override
    public Set<String> getUserAllOrgId(String userName) {
        return ComponentFactory.getUserAdaptor().getUserAllOrgId(userName);
    }

    @Override
    public List<User> getUserByOrg(String key) {
        return ComponentFactory.getUserAdaptor().getUserByOrg(key);
    }

    @Override
    public List<Organization> getOrganizationTree() {
        return ComponentFactory.getUserAdaptor().getOrganizationTree();
    }

    @Override
    public void register(UserReq userReq) {
        ComponentFactory.getUserAdaptor().register(userReq);
    }

    @Override
    public String login(UserReq userReq) {
        return ComponentFactory.getUserAdaptor().login(userReq);
    }

}
