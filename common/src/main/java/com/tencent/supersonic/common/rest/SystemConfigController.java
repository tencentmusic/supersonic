package com.tencent.supersonic.common.rest;

import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.service.CurrentUserProvider;
import com.tencent.supersonic.common.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/semantic/parameter"})
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService sysConfigService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public Boolean save(@RequestBody SystemConfig systemConfig, HttpServletRequest request,
            HttpServletResponse response) {
        User user = currentUserProvider.getCurrentUser(request, response);
        if (user.getIsAdmin() != 1) {
            throw new AccessException("only admin can modify system config");
        }
        sysConfigService.save(systemConfig);
        return true;
    }

    @GetMapping
    public SystemConfig get() {
        return sysConfigService.getSystemConfig();
    }
}
