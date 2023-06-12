package com.tencent.supersonic.semantic.query.domain.parser.convertor.sql.node;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class AggFunctionNode extends SemanticNode {

    public static enum AggFunction {
        AVG,
        COUNT_DISTINCT,
        MAX,
        MIN,
        SUM,
        COUNT,
        DISTINCT
    }

    public static SqlNode build(String agg, String name, SqlValidatorScope scope) throws Exception {
        if (AggFunction.COUNT_DISTINCT.name().equalsIgnoreCase(agg)) {
            return parse(AggFunction.COUNT.name() + " ( " + AggFunction.DISTINCT.name() + " " + name + " ) ", scope);
        }
        return parse(agg + " ( " + name + " ) ", scope);
    }


}
