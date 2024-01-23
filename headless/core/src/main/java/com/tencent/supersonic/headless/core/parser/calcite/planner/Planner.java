package com.tencent.supersonic.headless.core.parser.calcite.planner;


import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * parse and generate SQL and other execute information
 */
public interface Planner {

    public void explain(QueryStatement queryStatement, AggOption aggOption) throws Exception;

    public String getSql(EngineType enginType);

    public String getSourceId();

    public String simplify(String sql, EngineType engineType);
}
