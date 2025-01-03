package com.tencent.supersonic.headless.server.pojo.yaml;

import com.tencent.supersonic.common.pojo.enums.DataTypeEnums;
import com.tencent.supersonic.headless.api.pojo.DimensionTimeTypeParams;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DimensionYamlTpl {

    private String name;

    private String bizName;

    private String owners;

    private String type;

    private String expr;

    private DimensionTimeTypeParams typeParams;

    private DataTypeEnums dataType;

    private List<String> defaultValues;

    private Map<String, Object> ext;
}
