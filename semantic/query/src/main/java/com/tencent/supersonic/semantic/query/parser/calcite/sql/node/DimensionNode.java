package com.tencent.supersonic.semantic.query.parser.calcite.sql.node;


import com.tencent.supersonic.semantic.query.parser.calcite.dsl.Dimension;
import java.util.List;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class DimensionNode extends SemanticNode {

    public static SqlNode build(Dimension dimension, SqlValidatorScope scope) throws Exception {
        SqlNode sqlNode = parse(dimension.getExpr(), scope);
        return buildAs(dimension.getName(), sqlNode);
    }

    public static List<SqlNode> expand(Dimension dimension, SqlValidatorScope scope) throws Exception {
        SqlNode sqlNode = parse(dimension.getExpr(), scope);
        return expand(sqlNode, scope);
    }

    public static SqlNode buildName(Dimension dimension, SqlValidatorScope scope) throws Exception {
        return parse(dimension.getName(), scope);
    }

    public static SqlNode buildExp(Dimension dimension, SqlValidatorScope scope) throws Exception {
        return parse(dimension.getExpr(), scope);
    }

    public static SqlNode buildNameAs(String alias, Dimension dimension, SqlValidatorScope scope) throws Exception {
        if ("".equals(alias)) {
            return buildName(dimension, scope);
        }
        SqlNode sqlNode = parse(dimension.getName(), scope);
        return buildAs(alias, sqlNode);
    }


}
