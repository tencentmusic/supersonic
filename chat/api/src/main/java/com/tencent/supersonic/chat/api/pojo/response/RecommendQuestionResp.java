package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestionReq;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecommendQuestionResp {
    private Long domainId;
    private List<RecommendedQuestionReq> recommendedQuestions;
}
