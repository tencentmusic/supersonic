package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import lombok.Data;

@Data
public class ChatMemoryCreateReq {

    private Integer agentId;

    private String question;

    private String dbSchema;

    private String s2sql;

    private MemoryStatus status;
}
