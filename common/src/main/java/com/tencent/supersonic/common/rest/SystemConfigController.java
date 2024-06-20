package com.tencent.supersonic.common.rest;

import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/semantic/parameter"})
public class SystemConfigController {

    @Autowired
    private SystemConfigService sysConfigService;

    @PostMapping
    public Boolean save(@RequestBody SystemConfig systemConfig) {
        sysConfigService.save(systemConfig);
        return true;
    }

    @GetMapping
    public SystemConfig get() {
        return sysConfigService.getSystemConfig();
    }

}
