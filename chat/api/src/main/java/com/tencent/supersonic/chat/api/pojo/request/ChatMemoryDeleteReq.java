package com.tencent.supersonic.chat.api.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryDeleteReq {

    private List<Long> ids;

    private Integer agentId;
}
