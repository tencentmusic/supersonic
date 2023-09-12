package com.tencent.supersonic.knowledge.dictionary;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class DimValue2DictCommand {

    private DictUpdateMode updateMode;

    private List<Long> modelIds;

    private Map<Long, List<Long>> modelAndDimPair = new HashMap<>();
}