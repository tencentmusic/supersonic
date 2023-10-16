package com.tencent.supersonic.chat.api.pojo.request;


import lombok.Data;

@Data
public class RecommendReq {

    private Long modelId;

    private Long metricId;

}