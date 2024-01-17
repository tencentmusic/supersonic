package com.tencent.supersonic.headless.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatabaseParameter {

    private String name;
    private String comment;
    private String placeholder;
    private String value;
    private String dataType = "string";
    private Boolean require = true;
}
