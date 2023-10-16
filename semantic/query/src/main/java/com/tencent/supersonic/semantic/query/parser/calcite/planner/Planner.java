package com.tencent.supersonic.semantic.query.parser.calcite.planner;


import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;

public interface Planner {

    public void explain(MetricReq metricCommand, AggOption aggOption) throws Exception;

    public String getSql();

    public String getSourceId();
}
