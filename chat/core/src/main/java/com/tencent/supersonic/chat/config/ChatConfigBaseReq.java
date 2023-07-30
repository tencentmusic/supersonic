package com.tencent.supersonic.chat.config;

import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestion;
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
    private ChatDetailConfig chatDetailConfig;

    /**
     * the chatAggConfig about the domain
     */
    private ChatAggConfig chatAggConfig;


    /**
     * the recommended questions about the domain
     */
    private List<RecommendedQuestion> recommendedQuestions;

    /**
     * available status
     */
    private StatusEnum status;

}
