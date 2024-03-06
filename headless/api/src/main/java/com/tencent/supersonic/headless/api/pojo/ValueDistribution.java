package com.tencent.supersonic.headless.api.pojo;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class ValueDistribution {

    private Object valueMap;

    private Long totalCount;

    private Long valueCount;

    private Double ratio;
}