package com.tencent.supersonic.chat.config;

import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestion;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class ChatConfigRich {

    private Long id;

    private Long domainId;

    private String domainName;
    private String bizName;

    private ChatAggRichConfig chatAggRichConfig;

    private ChatDetailRichConfig chatDetailRichConfig;

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
