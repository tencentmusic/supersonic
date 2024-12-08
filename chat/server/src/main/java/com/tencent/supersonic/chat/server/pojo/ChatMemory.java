package com.tencent.supersonic.chat.server.pojo;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChatMemory {
    private Long id;

    private Integer agentId;

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
