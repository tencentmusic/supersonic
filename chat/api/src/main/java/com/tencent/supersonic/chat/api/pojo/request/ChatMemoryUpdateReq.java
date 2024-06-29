package com.tencent.supersonic.chat.api.pojo.request;


import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ChatMemoryUpdateReq extends RecordInfo {

    @NotNull(message = "id不可为空")
    private Long id;

    private String dbSchema;

    private String s2sql;

    private MemoryStatus status;

    private MemoryReviewResult humanReviewRet;

    private String humanReviewCmt;

}
