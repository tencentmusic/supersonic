package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import lombok.*;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChatMemory {
    private Long id;

    private Integer agentId;

    private Long queryId;

    private String question;

    private String sideInfo;

    private String dbSchema;

    private String s2sql;

    private MemoryStatus status;

    private MemoryReviewResult llmReviewRet;

    private String llmReviewCmt;

    private MemoryReviewResult humanReviewRet;

    private String humanReviewCmt;

    private String createdBy;

    private Date createdAt;

    private String updatedBy;

    private Date updatedAt;
}
