package com.tencent.supersonic.headless.query.parser.calcite.planner;


import com.tencent.supersonic.headless.common.query.enums.AggOption;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.query.parser.calcite.schema.HeadlessSchema;

public interface Planner {

    public void explain(QueryStatement queryStatement, AggOption aggOption) throws Exception;

    public String getSql();

    public String getSourceId();

    public HeadlessSchema findBest();
}
