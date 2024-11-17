package com.tencent.supersonic.headless.core.translator.calcite.planner;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/** parse and generate SQL and other execute information */
public interface Planner {

    void plan(QueryStatement queryStatement, AggOption aggOption) throws Exception;

    String getSql(EngineType enginType);

    String getSourceId();

    String simplify(String sql, EngineType engineType);
}
