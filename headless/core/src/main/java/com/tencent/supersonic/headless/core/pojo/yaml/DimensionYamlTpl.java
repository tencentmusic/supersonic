package com.tencent.supersonic.headless.core.pojo.yaml;


import com.tencent.supersonic.common.pojo.enums.DataTypeEnums;
import java.util.List;
import lombok.Data;


@Data
public class DimensionYamlTpl {

    private String name;

    private String bizName;

    private String owners;

    private String type;

    private String expr;

    private DimensionTimeTypeParamsTpl typeParams;

    private DataTypeEnums dataType;

    private List<String> defaultValues;
}
