package com.tencent.supersonic.headless.chat.knowledge;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
@Builder
public class DataSetInfoStat implements Serializable {

    private long dataSetCount;

    private long metricDataSetCount;

    private long dimensionDataSetCount;

    private long dimensionValueDataSetCount;

}