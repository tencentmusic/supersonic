package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Parameter {
    private String name;
    private String value;
    private String comment;
    private String description;
    private String dataType;
    private String module;
    private List<Object> candidateValues;

    public Parameter(String name, String value, String comment, String dataType, String module) {
        this.name = name;
        this.value = value;
        this.comment = comment;
        this.dataType = dataType;
        this.module = module;
    }

    public Parameter(String name, String value, String comment, String description, String dataType, String module) {
        this.name = name;
        this.value = value;
        this.comment = comment;
        this.description = description;
        this.dataType = dataType;
        this.module = module;
    }

}
