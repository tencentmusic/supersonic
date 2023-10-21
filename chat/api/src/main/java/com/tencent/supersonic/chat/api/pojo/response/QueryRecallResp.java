package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class QueryRecallResp {
    private List<SolvedQueryRecallResp> solvedQueryRecallRespList;
    private Long queryTimeCost;
}
