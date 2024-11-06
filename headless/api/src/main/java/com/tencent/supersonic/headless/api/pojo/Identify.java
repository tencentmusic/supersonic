package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Identify {

    private String name;

    /** primary, foreign */
    private String type;

    private String bizName;

    private Integer isCreateDimension = 0;

    public Identify(String name, String type, String bizName) {
        this.name = name;
        this.type = type;
        this.bizName = bizName;
    }

    public String getFieldName() {
        return bizName;
    }
}
