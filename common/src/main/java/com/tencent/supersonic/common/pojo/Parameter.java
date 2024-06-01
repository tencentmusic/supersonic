package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Parameter {
    private String name;
    private String defaultValue;
    private String comment;
    private String description;
    private String dataType;
    private String module;
    private String value;
    private List<Object> candidateValues;

    public Parameter(String name, String defaultValue, String comment,
                     String description, String dataType, String module) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.description = description;
        this.dataType = dataType;
        this.module = module;
    }

    public Parameter(String name, String defaultValue, String comment, String description,
                     String dataType, String module, List<Object> candidateValues) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.description = description;
        this.dataType = dataType;
        this.module = module;
        this.candidateValues = candidateValues;
    }

    public String getValue() {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

}
