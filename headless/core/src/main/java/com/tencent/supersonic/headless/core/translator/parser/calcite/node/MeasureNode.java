package com.tencent.supersonic.headless.core.translator.parser.calcite.node;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.Measure;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class MeasureNode extends SemanticNode {

    public static SqlNode buildNonAgg(String alias, Measure measure, SqlValidatorScope scope,
            EngineType engineType) throws Exception {
        return getExpr(measure, alias, scope, engineType);
    }

    private static SqlNode getExpr(Measure measure, String alias, SqlValidatorScope scope,
            EngineType enginType) throws Exception {
        if (measure.getExpr() == null) {
            return parse((alias.isEmpty() ? "" : alias + ".") + measure.getName(), scope,
                    enginType);
        }
        return parse(measure.getExpr(), scope, enginType);
    }
}
