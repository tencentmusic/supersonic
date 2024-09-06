package com.tencent.supersonic.common.jsqlparser;

import lombok.Data;

@Data
public class FieldExpression {

    private String operator;

    private String fieldName;

    private Object fieldValue;

    private String function;
}
