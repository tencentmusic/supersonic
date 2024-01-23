package com.tencent.supersonic.headless.core.parser.calcite.sql.node;


import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Dimension;
import java.util.List;
import java.util.Objects;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

public class DimensionNode extends SemanticNode {

    public static SqlNode build(Dimension dimension, SqlValidatorScope scope, EngineType engineType) throws Exception {
        SqlNode sqlNode = parse(dimension.getExpr(), scope, engineType);
        return buildAs(dimension.getName(), sqlNode);
    }

    public static List<SqlNode> expand(Dimension dimension, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        SqlNode sqlNode = parse(dimension.getExpr(), scope, engineType);
        return expand(sqlNode, scope);
    }

    public static SqlNode buildName(Dimension dimension, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        return parse(dimension.getName(), scope, engineType);
    }

    public static SqlNode buildExp(Dimension dimension, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        return parse(dimension.getExpr(), scope, engineType);
    }

    public static SqlNode buildNameAs(String alias, Dimension dimension, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        if ("".equals(alias)) {
            return buildName(dimension, scope, engineType);
        }
        SqlNode sqlNode = parse(dimension.getName(), scope, engineType);
        return buildAs(alias, sqlNode);
    }

    public static SqlNode buildArray(Dimension dimension, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        if (Objects.nonNull(dimension.getDataType()) && dimension.getDataType().isArray()) {
            SqlNode sqlNode = parse(dimension.getExpr(), scope, engineType);
            if (isIdentifier(sqlNode)) {
                return buildAs(dimension.getName(),
                        parse(dimension.getExpr() + Constants.DIMENSION_ARRAY_SINGLE_SUFFIX, scope, engineType));
            }
            throw new Exception("array dimension expr should only identify");
        }
        return build(dimension, scope, engineType);
    }

}
