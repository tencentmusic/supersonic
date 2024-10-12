package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ModelSchema {

    private String name;

    private String bizName;

    private String description;

    private List<FieldSchema> filedSchemas;

}
