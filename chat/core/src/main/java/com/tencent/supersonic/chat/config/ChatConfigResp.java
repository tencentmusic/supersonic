package com.tencent.supersonic.chat.config;

import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestion;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class ChatConfigResp {

    private Long id;

    private Long domainId;

    private ChatDetailConfig chatDetailConfig;

    private ChatAggConfig chatAggConfig;

    private List<RecommendedQuestion> recommendedQuestions;

    /**
     * available status
     */
    private StatusEnum statusEnum;

    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}