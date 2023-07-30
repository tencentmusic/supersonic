package com.tencent.supersonic.chat.mapper;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class DomainInfoStat implements Serializable {

    private long domainCount;

    private long metricDomainCount;

    private long dimensionDomainCount;

    private long dimensionValueDomainCount;

}