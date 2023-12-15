package com.tencent.supersonic.chat.api.pojo.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimilarQueryRecallResp {

    private Long queryId;

    private Integer parseId;

    private String queryText;

}