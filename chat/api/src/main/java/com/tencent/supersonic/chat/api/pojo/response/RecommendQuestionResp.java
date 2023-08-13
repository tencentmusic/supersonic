package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestionReq;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendQuestionResp {

    private Long modelId;
    private List<RecommendedQuestionReq> recommendedQuestions;
}
