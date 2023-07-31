package com.tencent.supersonic.chat.config;

import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestion;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class ChatConfig {

    /**
     * database auto-increment primary key
     */
    private Long id;

    private Long domainId;

    /**
     * the chatDetailConfig about the domain
     */
    private ChatDetailConfig chatDetailConfig;

    /**
     * the chatAggConfig about the domain
     */
    private ChatAggConfig chatAggConfig;

    private List<RecommendedQuestion> recommendedQuestions;

    /**
     * available status
     */
    private StatusEnum status;

    /**
     * about createdBy, createdAt, updatedBy, updatedAt
     */
    private RecordInfo recordInfo;

}
