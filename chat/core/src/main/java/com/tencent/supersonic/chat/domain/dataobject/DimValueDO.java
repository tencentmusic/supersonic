package com.tencent.supersonic.chat.domain.dataobject;


import com.tencent.supersonic.chat.domain.pojo.config.DefaultMetric;
import com.tencent.supersonic.chat.domain.pojo.config.Dim4Dict;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class DimValueDO {

    private Long domainId;

    private List<DefaultMetric> defaultMetricDescList = new ArrayList<>();

    private List<Dim4Dict> dimensions = new ArrayList<>();

    public DimValueDO setDomainId(Long domainId) {
        this.domainId = domainId;
        return this;
    }

    public DimValueDO setDefaultMetricIds(List<DefaultMetric> defaultMetricDescList) {
        this.defaultMetricDescList = defaultMetricDescList;
        return this;
    }

    public DimValueDO setDimensions(List<Dim4Dict> dimensions) {
        this.dimensions = dimensions;
        return this;
    }
}