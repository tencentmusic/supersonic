package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.authentication.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.SystemConfig;
import com.tencent.supersonic.common.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private SystemConfigService sysParameterService;

    public UserServiceImpl(SystemConfigService sysParameterService) {
        this.sysParameterService = sysParameterService;
    }

    @Override
    public User getCurrentUser(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        if (user != null) {
            SystemConfig systemConfig = sysParameterService.getSystemConfig();
            if (!CollectionUtils.isEmpty(systemConfig.getAdmins())
                    && systemConfig.getAdmins().contains(user.getName())) {
                user.setIsAdmin(1);
            }
        }
        return user;
    }

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
