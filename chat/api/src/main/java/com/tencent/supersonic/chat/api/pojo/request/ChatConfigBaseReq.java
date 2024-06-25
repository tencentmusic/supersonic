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
     * the recommended questions about the model
     * 关于模型的推荐问题
     */
    private List<RecommendedQuestionReq> recommendedQuestions;

    /**
     *  the llm examples about the model
     *  关于这个模型的法学硕士例子
     */
    private String llmExamples;

    /**
     * available status
     */
    private StatusEnum status;

}
