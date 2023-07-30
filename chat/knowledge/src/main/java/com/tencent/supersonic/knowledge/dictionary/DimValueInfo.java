package com.tencent.supersonic.knowledge.dictionary;


import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import java.util.List;
import javax.validation.constraints.NotNull;

public class DimValueInfo {

    /**
     * metricId、DimensionId、domainId
     */
    private Long itemId;

    /**
     * type: IntentionTypeEnum
     * temporarily only supports dimension-related information
     */
    @NotNull
    private TypeEnums type = TypeEnums.DIMENSION;

    private List<String> blackList;
    private List<String> whiteList;
    private List<String> ruleList;
    private Boolean isDictInfo;
}
