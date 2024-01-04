package com.tencent.supersonic.headless.core.pojo.yaml;


import lombok.Data;

import java.util.List;


@Data
public class MetricYamlTpl {

    private String name;

    private List<String> owners;

    private String type;

    private MetricTypeParamsYamlTpl typeParams;


}
