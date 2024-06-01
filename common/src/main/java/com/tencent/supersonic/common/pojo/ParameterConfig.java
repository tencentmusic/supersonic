package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public abstract class ParameterConfig {

    @Autowired
    private SystemConfigService sysConfigService;

    @Autowired
    private Environment environment;

    protected abstract List<Parameter> getSysParameters();

    /**
     * Parameter value will be derived by following orders:
     * 1. `system config` set with user interface
     * 2. `system property` set with application.yaml
     * 3. `default value` set with parameter declaration
     * @param parameter
     * @return parameter value
     */
    public String getParameterValue(Parameter parameter) {
        String paramName = parameter.getName();
        String value = sysConfigService.getSysParameter().getParameterByName(paramName);
        try {
            if (StringUtils.isBlank(value)) {
                if (environment.containsProperty(paramName)) {
                    value = environment.getProperty(paramName);
                } else {
                    value = parameter.getDefaultValue();
                }
            }
        } catch (Exception e) {
            log.error("Failed to get parameter value for {} with exception: {}", paramName, e);
        }

        return value;
    }
}
