package com.tencent.supersonic.headless.api.pojo;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DefaultDisplayInfo {

    //When displaying tag selection results, the information displayed by default
    private List<Long> dimensionIds = new ArrayList<>();

    private List<Long> metricIds = new ArrayList<>();

}
