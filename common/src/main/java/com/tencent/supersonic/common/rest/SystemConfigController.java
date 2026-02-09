package com.tencent.supersonic.common.rest;

import com.tencent.supersonic.common.config.SystemConfig;
import com.tencent.supersonic.common.service.SystemConfigService;
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
