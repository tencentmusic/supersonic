package com.tencent.supersonic.common.pojo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class SystemConfig {

    private Integer id;

    private List<String> admins;

    private List<Parameter> parameters;

    public String getAdmin() {
        if (CollectionUtils.isEmpty(admins)) {
            return "";
        }
        return StringUtils.join(admins, ",");
    }

    public String getParameterByName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        Map<String, String> nameToValue = getParameters().stream()
                .collect(Collectors.toMap(Parameter::getName, Parameter::getValue, (k1, k2) -> k1));
        return nameToValue.get(name);
    }

    public void setAdminList(String admin) {
        if (StringUtils.isNotBlank(admin)) {
            admins = Arrays.asList(admin.split(","));
        } else {
            admins = Lists.newArrayList();
        }
    }

    public void init() {
        parameters = buildDefaultParameters();
        admins = Lists.newArrayList("admin");
    }

    private List<Parameter> buildDefaultParameters() {
        List<Parameter> defaultParameters = Lists.newArrayList();
        Collection<ParameterConfig> configurableParameters =
                ContextUtils.getBeansOfType(ParameterConfig.class).values();
        for (ParameterConfig configParameters : configurableParameters) {
            defaultParameters.addAll(configParameters.getSysParameters());
        }
        return defaultParameters;
    }

    public List<Parameter> getParameters() {
        List<Parameter> defaultParameters = buildDefaultParameters();
        if (CollectionUtils.isEmpty(parameters)) {
            return defaultParameters;
        }
        Map<String, String> parameterNameValueMap = parameters.stream()
                .collect(Collectors.toMap(Parameter::getName, Parameter::getValue, (v1, v2) -> v2));
        for (Parameter parameter : defaultParameters) {
            parameter.setValue(parameterNameValueMap.getOrDefault(parameter.getName(),
                    parameter.getDefaultValue()));
        }
        return defaultParameters;
    }

}
