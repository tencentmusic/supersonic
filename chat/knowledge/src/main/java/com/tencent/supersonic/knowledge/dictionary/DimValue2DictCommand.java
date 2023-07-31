package com.tencent.supersonic.knowledge.dictionary;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.supersonic.knowledge.dictionary.DictUpdateMode;
import lombok.Data;

@Data
public class DimValue2DictCommand {

    private DictUpdateMode updateMode;

    private List<Long> domainIds;

    private Map<Long, List<Long>> domainAndDimPair = new HashMap<>();
}