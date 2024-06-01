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
        Map<String, String> nameToValue = parameters.stream()
                .collect(Collectors.toMap(Parameter::getName, Parameter::getDefaultValue, (k1, k2) -> k1));
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
        parameters = Lists.newArrayList();
        admins = Lists.newArrayList("admin");

        Collection<ParameterConfig> configurableParameters =
                ContextUtils.getBeansOfType(ParameterConfig.class).values();
        for (ParameterConfig configParameters : configurableParameters) {
            parameters.addAll(configParameters.getSysParameters());
        }
    }

}
