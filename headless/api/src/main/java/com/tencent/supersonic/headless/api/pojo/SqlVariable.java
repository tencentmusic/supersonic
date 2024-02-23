package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.enums.VariableValueType;
import lombok.Data;

import java.util.List;

@Data
public class SqlVariable {
    private String name;
    private VariableValueType valueType;
    private List<Object> defaultValues = Lists.newArrayList();
}


