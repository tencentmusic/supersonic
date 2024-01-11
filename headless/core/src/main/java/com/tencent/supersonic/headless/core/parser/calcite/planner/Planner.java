package com.tencent.supersonic.headless.core.parser.calcite.planner;


import com.tencent.supersonic.headless.api.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * parse and generate SQL and other execute information
 */
public interface Planner {

    public void explain(QueryStatement queryStatement, AggOption aggOption) throws Exception;

    public String getSql();

    public String getSourceId();

    public String simplify(String sql);
}
