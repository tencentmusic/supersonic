package com.tencent.supersonic.headless.core.translator.calcite.sql.node;


import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Measure;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class MeasureNode extends SemanticNode {

    public static SqlNode buildNonAgg(String alias, Measure measure, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        return buildAs(measure.getName(), getExpr(measure, alias, scope, engineType));
    }

    public static SqlNode buildAgg(Measure measure, boolean noAgg, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        if ((measure.getAgg() == null || measure.getAgg().isEmpty()) || noAgg) {
            return parse(measure.getName(), scope, engineType);
        }
        return buildAs(measure.getName(),
                AggFunctionNode.build(measure.getAgg(), measure.getName(), scope, engineType));
    }

    private static SqlNode getExpr(Measure measure, String alias, SqlValidatorScope scope, EngineType enginType)
            throws Exception {
        if (measure.getExpr() == null) {
            return parse((alias.isEmpty() ? "" : alias + ".") + measure.getName(), scope, enginType);
        }
        return parse(measure.getExpr(), scope, enginType);
    }

}
