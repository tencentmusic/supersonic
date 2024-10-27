package com.tencent.supersonic.headless.api.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class ModelSchema {

    private String name;

    private String bizName;

    private String description;

    private List<FieldSchema> filedSchemas;

    @JsonIgnore
    public FieldSchema getFieldByName(String columnName) {
        for (FieldSchema fieldSchema : filedSchemas) {
            if (fieldSchema.getColumnName().equalsIgnoreCase(columnName)) {
                return fieldSchema;
            }
        }
        return null;
    }

}
