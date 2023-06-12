package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.TypeEnums;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * information about dictionary about the domain
 */

@Data
public class KnowledgeInfo {

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