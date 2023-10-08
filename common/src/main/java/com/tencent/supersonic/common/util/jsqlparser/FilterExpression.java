package com.tencent.supersonic.common.util.jsqlparser;

import lombok.Data;

@Data
public class FilterExpression {

    private String operator;

    private String fieldName;

    private Object fieldValue;

    private String function;

}