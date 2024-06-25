package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * information about dictionary about the model
 */

@Data
public class KnowledgeInfoReq {

    /**
     * metricId、DimensionId、modelId
     */
    private Long itemId;

    private String bizName;
    /**
     * type: IntentionTypeEnum
     * temporarily only supports dimension-related information
     * *类型:IntentionTypeEnum
     * *暂时只支持维度相关信息
     */
    @NotNull
    private TypeEnums type = TypeEnums.DIMENSION;

    private Boolean searchEnable = false;

    /**
     * advanced knowledge config for single item
     */
    private KnowledgeAdvancedConfig knowledgeAdvancedConfig;
}
