package com.tencent.supersonic.chat.server.persistence.dataobject;


import com.tencent.supersonic.headless.core.config.DefaultMetric;
import com.tencent.supersonic.headless.core.config.Dim4Dict;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;


@Data
@ToString
public class DimValueDO {

    private Long modelId;

    private List<DefaultMetric> defaultMetricDescList = new ArrayList<>();

    private List<Dim4Dict> dimensions = new ArrayList<>();

    public DimValueDO setModelId(Long modelId) {
        this.modelId = modelId;
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