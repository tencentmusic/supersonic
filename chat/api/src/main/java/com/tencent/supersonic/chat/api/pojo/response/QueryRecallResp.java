package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class QueryRecallResp {
    private List<SimilarQueryRecallResp> solvedQueryRecallRespList;
    private Long queryTimeCost;
}
