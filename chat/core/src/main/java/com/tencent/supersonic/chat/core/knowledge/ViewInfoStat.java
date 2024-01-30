package com.tencent.supersonic.chat.core.knowledge;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
@Builder
public class ViewInfoStat implements Serializable {

    private long viewCount;

    private long metricViewCount;

    private long dimensionViewCount;

    private long dimensionValueViewCount;

}