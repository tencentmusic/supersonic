package com.tencent.supersonic.semantic.query.domain.parser.dsl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Identify {

    private String name;

    // primary or foreign
    private String type;
}
