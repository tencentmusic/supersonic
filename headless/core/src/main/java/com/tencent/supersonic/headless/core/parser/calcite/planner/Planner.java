package com.tencent.supersonic.headless.core.parser.calcite.planner;


import com.tencent.supersonic.headless.common.core.enums.AggOption;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.parser.calcite.schema.HeadlessSchema;

public interface Planner {

    public void explain(QueryStatement queryStatement, AggOption aggOption) throws Exception;

    public String getSql();

    public String getSourceId();

    public HeadlessSchema findBest();
}
