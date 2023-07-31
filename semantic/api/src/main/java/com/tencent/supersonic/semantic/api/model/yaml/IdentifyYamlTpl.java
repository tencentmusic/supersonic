package com.tencent.supersonic.semantic.api.model.yaml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdentifyYamlTpl {

    private String name;

    /**
     * 主键 primary 外键 foreign
     */
    private String type;

}
