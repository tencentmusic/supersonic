package com.tencent.supersonic.auth.api.authentication.utils;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.common.pojo.SysParameter;
import com.tencent.supersonic.common.service.SysParameterService;
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
        SysParameterService sysParameterService = ContextUtils.getBean(SysParameterService.class);
        SysParameter sysParameter = sysParameterService.getSysParameter();
        if (!CollectionUtils.isEmpty(sysParameter.getAdmins())
                && sysParameter.getAdmins().contains(user.getName())) {
            user.setIsAdmin(1);
        }
        return user;
    }

}
