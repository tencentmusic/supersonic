package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.request.KnowledgeAdvancedConfig;
import com.tencent.supersonic.chat.api.pojo.request.KnowledgeInfoReq;
import java.util.List;
import lombok.Data;

@Data
public class ChatDetailRichConfigResp {

    /**
     * invisible dimensions/metrics
     */
    private ItemVisibilityInfo visibility;

    /**
     * information about dictionary about the model
     */
    private List<KnowledgeInfoReq> knowledgeInfos;

    private KnowledgeAdvancedConfig globalKnowledgeConfig;

    private ChatDefaultRichConfigResp chatDefaultConfig;


}