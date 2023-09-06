package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestionReq;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class ChatConfigRichResp {

    private Long id;

    private Long modelId;

    private String modelName;
    private String bizName;

    private ChatAggRichConfigResp chatAggRichConfig;

    private ChatDetailRichConfigResp chatDetailRichConfig;

    private List<RecommendedQuestionReq> recommendedQuestions;

    /**
     * available status
     */
    private StatusEnum statusEnum;

    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}
