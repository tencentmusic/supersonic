package com.tencent.supersonic.semantic.api.core.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Identify {

    private String name;

    /**
     * like primary, foreign
     */
    private String type;

    private String bizName;

}
