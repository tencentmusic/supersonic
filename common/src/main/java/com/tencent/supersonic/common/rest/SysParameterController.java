package com.tencent.supersonic.common.rest;

import com.tencent.supersonic.common.pojo.SysParameter;
import com.tencent.supersonic.common.service.SysParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/semantic/parameter"})
public class SysParameterController {

    @Autowired
    private SysParameterService sysParameterService;

    @PostMapping
    public Boolean save(@RequestBody SysParameter sysParameter) {
        sysParameterService.save(sysParameter);
        return true;
    }

    @GetMapping
    public SysParameter get() {
        return sysParameterService.getSysParameter();
    }

}
