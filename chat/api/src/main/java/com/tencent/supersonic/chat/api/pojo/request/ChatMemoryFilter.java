package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemoryFilter {

    private Integer agentId;

    private String question;

    private List<String> questions;

    private MemoryStatus status;

    private MemoryReviewResult llmReviewRet;

    private MemoryReviewResult humanReviewRet;

}
