package com.tencent.supersonic.chat.server.config;

import com.tencent.supersonic.chat.api.pojo.request.ChatAggConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDetailConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestionReq;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class ChatConfig {

    /** database auto-increment primary key */
    private Long id;

    private Long modelId;

    /** the chatDetailConfig about the model */
    private ChatDetailConfigReq chatDetailConfig;

    /** the chatAggConfig about the model */
    private ChatAggConfigReq chatAggConfig;

    private List<RecommendedQuestionReq> recommendedQuestions;

    /** available status */
    private StatusEnum status;

    /** about createdBy, createdAt, updatedBy, updatedAt */
    private RecordInfo recordInfo;
}
