package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBColumn {

    private String columnName;

    private String dataType;

    private String comment;

    private FieldType fieldType;
}
