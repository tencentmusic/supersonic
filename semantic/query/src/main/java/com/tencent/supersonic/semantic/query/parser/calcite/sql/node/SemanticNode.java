package com.tencent.supersonic.semantic.query.parser.calcite.sql.node;


import com.tencent.supersonic.semantic.query.parser.calcite.Configuration;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.Optimization;
import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticSqlDialect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlAsOperator;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.commons.lang3.StringUtils;

public abstract class SemanticNode {

    public static SqlNode parse(String expression, SqlValidatorScope scope) throws Exception {
        SqlParser sqlParser = SqlParser.create(expression, Configuration.getParserConfig());
        SqlNode sqlNode = sqlParser.parseExpression();
        scope.validateExpr(sqlNode);
        return sqlNode;
    }

    public static SqlNode buildAs(String asName, SqlNode sqlNode) throws Exception {
        SqlAsOperator sqlAsOperator = new SqlAsOperator();
        SqlIdentifier sqlIdentifier = new SqlIdentifier(asName, SqlParserPos.ZERO);
        return new SqlBasicCall(sqlAsOperator, new ArrayList<>(Arrays.asList(sqlNode, sqlIdentifier)),
                SqlParserPos.ZERO);
    }

    public static String getSql(SqlNode sqlNode) {
        SqlWriterConfig config = SqlPrettyWriter.config().withDialect(SemanticSqlDialect.DEFAULT)
                .withKeywordsLowerCase(true).withClauseEndsLine(true).withAlwaysUseParentheses(false)
                .withSelectListItemsOnSeparateLines(false).withUpdateSetListNewline(false).withIndentation(0);

        UnaryOperator<SqlWriterConfig> sqlWriterConfigUnaryOperator = (c) -> config;
        return sqlNode.toSqlString(sqlWriterConfigUnaryOperator).getSql();
    }

    public static boolean isNumeric(String expr) {
        return StringUtils.isNumeric(expr);
    }

    public static List<SqlNode> expand(SqlNode sqlNode, SqlValidatorScope scope) throws Exception {
        if (!isIdentifier(sqlNode)) {
            List<SqlNode> sqlNodeList = new ArrayList<>();
            expand(sqlNode, sqlNodeList);
            return sqlNodeList;
        }
        return new ArrayList<>(Arrays.asList(sqlNode));
    }

    public static void expand(SqlNode sqlNode, List<SqlNode> sqlNodeList) {
        if (sqlNode instanceof SqlIdentifier) {
            sqlNodeList.add(sqlNode);
            return;
        }
        if (sqlNode instanceof SqlBasicCall) {
            SqlBasicCall sqlBasicCall = (SqlBasicCall) sqlNode;
            for (SqlNode sqlNo : sqlBasicCall.getOperandList()) {
                expand(sqlNo, sqlNodeList);
            }
        }
    }

    public static boolean isIdentifier(SqlNode sqlNode) {
        return sqlNode instanceof SqlIdentifier;
    }

    public static SqlNode getAlias(SqlNode sqlNode, SqlValidatorScope scope) throws Exception {
        if (sqlNode instanceof SqlBasicCall) {
            SqlBasicCall sqlBasicCall = (SqlBasicCall) sqlNode;
            if (sqlBasicCall.getKind().equals(SqlKind.AS) && sqlBasicCall.getOperandList().size() > 1) {
                return sqlBasicCall.getOperandList().get(1);
            }
        }
        if (sqlNode instanceof SqlIdentifier) {
            return sqlNode;
        }
        return null;
    }

    public static Set<String> getSelect(SqlNode sqlNode) {
        SqlNode table = getTable(sqlNode);
        if (table instanceof SqlSelect) {
            SqlSelect tableSelect = (SqlSelect) table;
            return tableSelect.getSelectList().stream()
                    .map(s -> (s instanceof SqlIdentifier) ? ((SqlIdentifier) s).names.get(0)
                            : (((s instanceof SqlBasicCall) && s.getKind().equals(SqlKind.AS))
                                    ? ((SqlBasicCall) s).getOperandList().get(1).toString() : ""))
                    .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    public static SqlNode getTable(SqlNode sqlNode) {
        if (sqlNode instanceof SqlBasicCall) {
            SqlBasicCall sqlBasicCall = (SqlBasicCall) sqlNode;
            if (sqlBasicCall.getOperator().getKind().equals(SqlKind.AS)) {
                if (sqlBasicCall.getOperandList().get(0) instanceof SqlSelect) {
                    SqlSelect table = (SqlSelect) sqlBasicCall.getOperandList().get(0);
                    return table;
                }
            }
        }
        return sqlNode;
    }

    public static RelNode getRelNode(CalciteSchema rootSchema, SqlToRelConverter sqlToRelConverter, String sql)
            throws SqlParseException {
        SqlValidator sqlValidator = Configuration.getSqlValidator(rootSchema);
        return sqlToRelConverter.convertQuery(
                sqlValidator.validate(SqlParser.create(sql, SqlParser.Config.DEFAULT).parseStmt()), false, true).rel;
    }

    public void accept(Optimization optimization) {
        optimization.visit(this);
    }


}
