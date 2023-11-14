package com.tencent.supersonic.semantic.query.parser.calcite.sql.node;


import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Measure;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class MeasureNode extends SemanticNode {

    public static SqlNode build(Measure measure, boolean noAgg, SqlValidatorScope scope) throws Exception {
        boolean addAgg = false;
        if (!noAgg && measure.getAgg() != null && !measure.getAgg().isEmpty()) {
            addAgg = true;
        }
        if (measure.getExpr() == null) {
            if (addAgg) {
                return parse(measure.getAgg() + " ( " + measure.getName() + " ) ", scope);
            }
            return parse(measure.getName(), scope);
        } else {
            if (addAgg) {
                return buildAs(measure.getName(), parse(measure.getAgg() + " ( " + measure.getExpr() + " ) ", scope));
            }
            return buildAs(measure.getName(), parse(measure.getExpr(), scope));
        }
    }


    public static SqlNode buildNonAgg(String alias, Measure measure, SqlValidatorScope scope) throws Exception {
        return buildAs(measure.getName(), getExpr(measure, alias, scope));
    }

    public static SqlNode buildAgg(Measure measure, boolean noAgg, SqlValidatorScope scope) throws Exception {
        if ((measure.getAgg() == null || measure.getAgg().isEmpty()) || noAgg) {
            return parse(measure.getName(), scope);
        }
        return buildAs(measure.getName(), AggFunctionNode.build(measure.getAgg(), measure.getName(), scope));
    }

    public static SqlNode buildAggAs(String aggFunc, String name, SqlValidatorScope scope) throws Exception {
        return buildAs(name, AggFunctionNode.build(aggFunc, name, scope));
    }

    private static SqlNode getExpr(Measure measure, String alias, SqlValidatorScope scope) throws Exception {
        if (measure.getExpr() == null) {
            return parse((alias.isEmpty() ? "" : alias + ".") + measure.getName(), scope);
        }
        return parse(measure.getExpr(), scope);
    }
}
