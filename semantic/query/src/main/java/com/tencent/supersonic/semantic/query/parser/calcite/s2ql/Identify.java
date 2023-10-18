package com.tencent.supersonic.semantic.query.parser.calcite.s2ql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Identify {

    public enum Type {
        PRIMARY, FOREIGN
    }

    private String name;

    // primary or foreign
    private String type;
}
