package com.tencent.supersonic.chat.domain.pojo.search;

import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DomainInfoStat implements Serializable {

    private long domainCount;
    private long metricDomainCount;
    private long dimensionDomainCount;
    private long dimensionValueDomainCount;

}