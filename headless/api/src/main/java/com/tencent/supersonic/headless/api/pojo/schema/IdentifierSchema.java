package com.tencent.supersonic.headless.api.pojo.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdentifierSchema {

    private String name;

    /** 主键 primary 外键 foreign */
    private String type;
}
