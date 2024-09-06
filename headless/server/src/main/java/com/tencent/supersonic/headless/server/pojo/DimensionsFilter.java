package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class DimensionsFilter {

    private List<Long> modelIds;

    private List<Long> dimensionIds;

    private List<String> dimensionNames;
}
