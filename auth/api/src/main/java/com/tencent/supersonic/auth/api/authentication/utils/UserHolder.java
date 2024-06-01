package com.tencent.supersonic.auth.api.authentication.utils;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.common.pojo.SystemConfig;
import com.tencent.supersonic.common.service.SystemConfigService;
import com.tencent.supersonic.common.util.ContextUtils;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class UserHolder {

    private static UserStrategy REPO;

    public static synchronized void setStrategy(UserStrategy strategy) {
        REPO = strategy;
    }

    public static User findUser(HttpServletRequest request, HttpServletResponse response) {
        User user = REPO.findUser(request, response);
        SystemConfigService sysParameterService = ContextUtils.getBean(SystemConfigService.class);
        SystemConfig systemConfig = sysParameterService.getSystemConfig();
        if (!CollectionUtils.isEmpty(systemConfig.getAdmins())
                && systemConfig.getAdmins().contains(user.getName())) {
            user.setIsAdmin(1);
        }
        return user;
    }

}
