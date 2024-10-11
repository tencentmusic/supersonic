package com.tencent.supersonic.headless.api.pojo;

import java.util.List;
import lombok.Data;

@Data
public class ModelSchema {

    private String name;

    private String bizName;

    private String description;

    private List<FieldSchema> filedSchemas;

}
