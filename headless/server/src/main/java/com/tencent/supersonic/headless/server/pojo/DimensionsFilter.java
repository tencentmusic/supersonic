package com.tencent.supersonic.headless.server.pojo;

import java.util.List;
import lombok.Data;

@Data
public class DimensionsFilter {

    private List<Long> modelIds;

    private List<Long> dimensionIds;

    private List<String> dimensionNames;

}