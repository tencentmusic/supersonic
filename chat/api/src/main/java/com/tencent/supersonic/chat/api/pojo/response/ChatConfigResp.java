package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.request.ChatAggConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDetailConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestionReq;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ChatConfigResp {

    private Long id;

    private Long modelId;

    private ChatDetailConfigReq chatDetailConfig;

    private ChatAggConfigReq chatAggConfig;

    private List<RecommendedQuestionReq> recommendedQuestions;

    private String llmExamples;

    /** available status */
    private StatusEnum statusEnum;

    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}
