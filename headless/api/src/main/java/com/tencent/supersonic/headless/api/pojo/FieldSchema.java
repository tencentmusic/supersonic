package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import lombok.Data;

@Data
public class FieldSchema {

    private String columnName;

    private String dataType;

    private String comment;

    private FieldType filedType;

    private String name;
}
