package com.tencent.supersonic.chat.api.pojo.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolvedQueryReq {

    private Long queryId;

    private Integer parseId;

    private String queryText;

    private Long modelId;

    private Integer agentId;

}