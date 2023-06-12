package com.tencent.supersonic.semantic.query.domain.parser.convertor.planner;


import com.tencent.supersonic.semantic.api.query.request.MetricReq;

public interface Planner {

    public void explain(MetricReq metricCommand, boolean isAgg) throws Exception;

    public String getSql();

    public String getSourceId();
}
