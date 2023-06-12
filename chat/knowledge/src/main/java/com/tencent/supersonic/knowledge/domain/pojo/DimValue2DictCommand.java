package com.tencent.supersonic.knowledge.domain.pojo;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class DimValue2DictCommand {

    private DictUpdateMode updateMode;

    private List<Long> domainIds;

    private Map<Long, List<Long>> domainAndDimPair = new HashMap<>();
}