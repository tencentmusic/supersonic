package com.tencent.supersonic.semantic.query.parser.calcite.planner;


import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;

public interface Planner {

    public void explain(QueryStatement queryStatement, AggOption aggOption) throws Exception;

    public String getSql();

    public String getSourceId();

    public SemanticSchema findBest();
}
