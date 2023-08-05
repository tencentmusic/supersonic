package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.StatusEnum;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * extended information command about domain
 */
@Data
@ToString
public class ChatConfigBaseReq {

    private Long domainId;

    /**
     * the chatDetailConfig about the domain
     */
    private ChatDetailConfigReq chatDetailConfig;

    /**
     * the chatAggConfig about the domain
     */
    private ChatAggConfigReq chatAggConfig;


    /**
     * the recommended questions about the domain
     */
    private List<RecommendedQuestionReq> recommendedQuestions;

    /**
     * available status
     */
    private StatusEnum status;

}
