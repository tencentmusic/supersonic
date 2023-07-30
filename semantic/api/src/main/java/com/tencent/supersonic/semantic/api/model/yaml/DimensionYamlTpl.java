package com.tencent.supersonic.semantic.api.model.yaml;


import lombok.Data;


@Data
public class DimensionYamlTpl {

    private String name;

    private String owners;

    private String type;

    private String expr;

    private DimensionTimeTypeParamsTpl typeParams;

}
