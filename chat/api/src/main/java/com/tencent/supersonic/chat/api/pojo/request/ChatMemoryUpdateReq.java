package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class ChatMemoryUpdateReq {

    @NotNull(message = "id不可为空")
    private Long id;

    private String dbSchema;

    private String s2sql;

    private MemoryStatus status;

    private MemoryReviewResult humanReviewRet;

    private String humanReviewCmt;
}
