package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.enums.VariableValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SqlVariable {
    private String name;
    private VariableValueType valueType;
    private List<Object> defaultValues = Lists.newArrayList();
}
