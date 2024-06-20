package com.tencent.supersonic.common.config;

import com.tencent.supersonic.common.pojo.Parameter;
import com.tencent.supersonic.common.service.SystemConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public abstract class ParameterConfig {

    @Autowired
    private SystemConfigService sysConfigService;

    @Autowired
    private Environment environment;

    /**
     * @return system parameters to be set with user interface
     */
    protected abstract List<Parameter> getSysParameters();

    /**
     * Parameter value will be derived in the following order:
     * 1. `system config` set with user interface
     * 2. `system property` set with application.yaml file
     * 3. `default value` set with parameter declaration
     * @param parameter instance
     * @return parameter value
     */
    public String getParameterValue(Parameter parameter) {
        String paramName = parameter.getName();
        String value = sysConfigService.getSystemConfig().getParameterByName(paramName);
        if (StringUtils.isBlank(value)) {
            if (environment.containsProperty(paramName)) {
                value = environment.getProperty(paramName);
            } else {
                value = parameter.getDefaultValue();
            }
        }

        return value;
    }
}
