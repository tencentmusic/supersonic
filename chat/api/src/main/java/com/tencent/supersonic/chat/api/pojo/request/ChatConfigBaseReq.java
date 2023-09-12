package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.StatusEnum;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * extended information command about model
 */
@Data
@ToString
public class ChatConfigBaseReq {

    private Long modelId;

    /**
     * the chatDetailConfig about the model
     */
    private ChatDetailConfigReq chatDetailConfig;

    /**
     * the chatAggConfig about the model
     */
    private ChatAggConfigReq chatAggConfig;


    /**
     * the recommended questions about the model
     */
    private List<RecommendedQuestionReq> recommendedQuestions;

    /**
     * available status
     */
    private StatusEnum status;

}
