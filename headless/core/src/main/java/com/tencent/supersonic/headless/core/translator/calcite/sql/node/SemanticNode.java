package com.tencent.supersonic.headless.core.translator.calcite.sql.node;


import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.core.translator.calcite.sql.optimizer.FilterToGroupScanRule;
import com.tencent.supersonic.headless.core.translator.calcite.Configuration;
import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSqlDialect;
import com.tencent.supersonic.headless.core.utils.SqlDialectFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlAsOperator;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlBinaryOperator;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.calcite.sql.validate.SqlValidatorWithHints;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * model item node
 */
@Slf4j
public abstract class SemanticNode {

    public static Set<SqlKind> AGGREGATION_KIND = new HashSet<>();
    public static Set<String> AGGREGATION_FUNC = new HashSet<>();
    public static List<String> groupHints = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9"));

    static {
        AGGREGATION_KIND.add(SqlKind.AVG);
        AGGREGATION_KIND.add(SqlKind.COUNT);
        AGGREGATION_KIND.add(SqlKind.SUM);
        AGGREGATION_KIND.add(SqlKind.MAX);
        AGGREGATION_KIND.add(SqlKind.MIN);
        AGGREGATION_KIND.add(SqlKind.OTHER_FUNCTION); //  more
        AGGREGATION_FUNC.add("sum");
        AGGREGATION_FUNC.add("count");
        AGGREGATION_FUNC.add("max");
        AGGREGATION_FUNC.add("avg");
        AGGREGATION_FUNC.add("min");
    }

    public static SqlNode parse(String expression, SqlValidatorScope scope, EngineType engineType) throws Exception {
        SqlValidatorWithHints sqlValidatorWithHints = Configuration.getSqlValidatorWithHints(
                scope.getValidator().getCatalogReader().getRootSchema(), engineType);
        if (Configuration.getSqlAdvisor(sqlValidatorWithHints, engineType).getReservedAndKeyWords()
                .contains(expression.toUpperCase())) {
            expression = String.format("`%s`", expression);
        }
        SqlParser sqlParser = SqlParser.create(expression, Configuration.getParserConfig(engineType));
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

    public static String getSql(SqlNode sqlNode, EngineType engineType) {
        UnaryOperator<SqlWriterConfig> sqlWriterConfigUnaryOperator = (c) -> getSqlWriterConfig(engineType);
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

    private static SqlWriterConfig getSqlWriterConfig(EngineType engineType) {
        SemanticSqlDialect sqlDialect = SqlDialectFactory.getSqlDialect(engineType);
        SqlWriterConfig config = SqlPrettyWriter.config().withDialect(sqlDialect)
                .withKeywordsLowerCase(false).withClauseEndsLine(true).withAlwaysUseParentheses(false)
                .withSelectListItemsOnSeparateLines(false).withUpdateSetListNewline(false).withIndentation(0);
        if (EngineType.MYSQL.equals(engineType)) {
            //no backticks around function name
            config = config.withQuoteAllIdentifiers(false);
        }
        return config;
    }

    private static void sqlVisit(SqlNode sqlNode, Map<String, Object> parseInfo) {
        SqlKind kind = sqlNode.getKind();
        switch (kind) {
            case SELECT:
                queryVisit(sqlNode, parseInfo);
                break;
            case AS:
                SqlBasicCall sqlBasicCall = (SqlBasicCall) sqlNode;
                if (sqlBasicCall.getOperandList().get(0).getKind().equals(SqlKind.IDENTIFIER)) {
                    addTableName(sqlBasicCall.getOperandList().get(0).toString(),
                            sqlBasicCall.getOperandList().get(1).toString(), parseInfo);
                } else {
                    sqlVisit(sqlBasicCall.getOperandList().get(0), parseInfo);
                }
                break;
            case JOIN:
                SqlJoin sqlJoin = (SqlJoin) sqlNode;
                sqlVisit(sqlJoin.getLeft(), parseInfo);
                sqlVisit(sqlJoin.getRight(), parseInfo);
                SqlBasicCall condition = (SqlBasicCall) sqlJoin.getCondition();
                if (Objects.nonNull(condition)) {
                    condition.getOperandList().stream().forEach(c -> addTagField(c.toString(), parseInfo, ""));
                }
                break;
            case UNION:
                ((SqlBasicCall) sqlNode).getOperandList().forEach(node -> {
                    sqlVisit(node, parseInfo);
                });
                break;
            case WITH:
                SqlWith sqlWith = (SqlWith) sqlNode;
                sqlVisit(sqlWith.body, parseInfo);
                break;
            default:
                break;
        }
    }

    private static void queryVisit(SqlNode select, Map<String, Object> parseInfo) {
        if (select == null) {
            return;
        }
        SqlSelect sqlSelect = (SqlSelect) select;
        SqlNodeList selectList = sqlSelect.getSelectList();
        selectList.getList().forEach(list -> {
            fieldVisit(list, parseInfo, "");
        });
        fromVisit(sqlSelect.getFrom(), parseInfo);
        if (sqlSelect.hasWhere()) {
            whereVisit((SqlBasicCall) sqlSelect.getWhere(), parseInfo);
        }
        if (sqlSelect.hasOrderBy()) {
            fieldVisit(sqlSelect.getOrderList(), parseInfo, "");
        }
        SqlNodeList group = sqlSelect.getGroup();
        if (group != null) {
            group.forEach(groupField -> {
                if (groupHints.contains(groupField.toString())) {
                    int groupIdx = Integer.valueOf(groupField.toString()) - 1;
                    if (selectList.getList().size() > groupIdx) {
                        fieldVisit(selectList.get(groupIdx), parseInfo, "");
                    }
                } else {
                    fieldVisit(groupField, parseInfo, "");
                }
            });
        }
    }

    private static void whereVisit(SqlBasicCall where, Map<String, Object> parseInfo) {
        if (where == null) {
            return;
        }
        if (where.operandCount() == 2 && where.operand(0).getKind().equals(SqlKind.IDENTIFIER)
                && where.operand(1).getKind().equals(SqlKind.LITERAL)) {
            fieldVisit(where.operand(0), parseInfo, "");
            return;
        }
        // 子查询
        if (where.operandCount() == 2
                && (where.operand(0).getKind().equals(SqlKind.IDENTIFIER)
                && (where.operand(1).getKind().equals(SqlKind.SELECT)
                || where.operand(1).getKind().equals(SqlKind.ORDER_BY)))
        ) {
            fieldVisit(where.operand(0), parseInfo, "");
            sqlVisit((SqlNode) (where.operand(1)), parseInfo);
            return;
        }
        if (CollectionUtils.isNotEmpty(where.getOperandList()) && where.operand(0).getKind()
                .equals(SqlKind.IDENTIFIER)) {
            fieldVisit(where.operand(0), parseInfo, "");
        }
        if (where.operandCount() >= 2 && where.operand(1).getKind().equals(SqlKind.IDENTIFIER)) {
            fieldVisit(where.operand(1), parseInfo, "");
        }
        if (CollectionUtils.isNotEmpty(where.getOperandList()) && where.operand(0) instanceof SqlBasicCall) {
            whereVisit(where.operand(0), parseInfo);
        }
        if (where.operandCount() >= 2 && where.operand(1) instanceof SqlBasicCall) {
            whereVisit(where.operand(1), parseInfo);
        }
    }

    private static void fieldVisit(SqlNode field, Map<String, Object> parseInfo, String func) {
        if (field == null) {
            return;
        }
        SqlKind kind = field.getKind();
        //System.out.println(kind);
        // aggfunction
        if (AGGREGATION_KIND.contains(kind)) {
            SqlOperator sqlCall = ((SqlCall) field).getOperator();
            if (AGGREGATION_FUNC.contains(sqlCall.toString().toLowerCase())) {
                List<SqlNode> operandList = ((SqlBasicCall) field).getOperandList();
                for (int i = 0; i < operandList.size(); i++) {
                    fieldVisit(operandList.get(i), parseInfo, sqlCall.toString().toUpperCase());
                }
                return;
            }
        }
        if (kind.equals(SqlKind.IDENTIFIER)) {
            addTagField(field.toString(), parseInfo, func);
            return;
        }
        if (kind.equals(SqlKind.AS)) {
            List<SqlNode> operandList1 = ((SqlBasicCall) field).getOperandList();
            SqlNode left = operandList1.get(0);
            fieldVisit(left, parseInfo, "");
            return;
        }
        if (field instanceof SqlBasicCall) {
            List<SqlNode> operandList = ((SqlBasicCall) field).getOperandList();
            for (int i = 0; i < operandList.size(); i++) {
                fieldVisit(operandList.get(i), parseInfo, "");
            }
        }
        if (field instanceof SqlNodeList) {
            ((SqlNodeList) field).getList().forEach(node -> {
                fieldVisit(node, parseInfo, "");
            });
        }
    }

    private static void addTagField(String exp, Map<String, Object> parseInfo, String func) {

        if (!parseInfo.containsKey(Constants.SQL_PARSER_FIELD)) {
            parseInfo.put(Constants.SQL_PARSER_FIELD, new HashMap<>());
        }
        Map<String, Set<String>> fields = (Map<String, Set<String>>) parseInfo.get(Constants.SQL_PARSER_FIELD);

        if (Pattern.matches("(?i)[a-z\\d_\\.]+", exp)) {
            if (exp.contains(".")) {
                String[] res = exp.split("\\.");
                if (!fields.containsKey(res[0])) {
                    fields.put(res[0], new HashSet<>());
                }
                fields.get(res[0]).add(res[1]);
            } else {
                if (!fields.containsKey("")) {
                    fields.put("", new HashSet<>());
                }
                fields.get("").add(exp);
            }

        }

    }

    private static void fromVisit(SqlNode from, Map<String, Object> parseInfo) {
        SqlKind kind = from.getKind();
        switch (kind) {
            case IDENTIFIER:
                SqlIdentifier sqlIdentifier = (SqlIdentifier) from;
                addTableName(sqlIdentifier.toString(), "", parseInfo);
                break;
            case AS:
                SqlBasicCall sqlBasicCall = (SqlBasicCall) from;
                SqlNode selectNode0 = sqlBasicCall.getOperandList().get(0);
                SqlNode selectNode1 = sqlBasicCall.getOperandList().get(1);
                if (!SqlKind.UNION.equals(selectNode0.getKind())) {
                    if (!SqlKind.SELECT.equals(selectNode0.getKind())) {
                        addTableName(selectNode0.toString(), selectNode1.toString(), parseInfo);
                    }
                }
                sqlVisit(selectNode0, parseInfo);
                break;
            case JOIN:
                SqlJoin sqlJoin = (SqlJoin) from;
                sqlVisit(sqlJoin.getLeft(), parseInfo);
                sqlVisit(sqlJoin.getRight(), parseInfo);
                SqlBasicCall condition = (SqlBasicCall) sqlJoin.getCondition();
                if (Objects.nonNull(condition)) {
                    condition.getOperandList().stream().forEach(c -> addTagField(c.toString(), parseInfo, ""));
                }
                break;
            case SELECT:
                sqlVisit(from, parseInfo);
                break;
            default:
                break;
        }
    }

    private static void addTableName(String exp, String alias, Map<String, Object> parseInfo) {
        if (exp.indexOf(" ") > 0) {
            return;
        }
        if (!parseInfo.containsKey(Constants.SQL_PARSER_TABLE)) {
            parseInfo.put(Constants.SQL_PARSER_TABLE, new HashMap<>());
        }
        Map<String, Set<String>> dbTbs = (Map<String, Set<String>>) parseInfo.get(Constants.SQL_PARSER_TABLE);
        if (!dbTbs.containsKey(alias)) {
            dbTbs.put(alias, new HashSet<>());
        }
        dbTbs.get(alias).add(exp);

    }

    public static Map<String, Object> getDbTable(SqlNode sqlNode) {
        Map<String, Object> parseInfo = new HashMap<>();
        sqlVisit(sqlNode, parseInfo);
        return parseInfo;
    }

    public static SqlNode optimize(SqlValidatorScope scope, SemanticSchema schema, SqlNode sqlNode,
                                   EngineType engineType) {
        try {
            HepProgramBuilder hepProgramBuilder = new HepProgramBuilder();
            SemanticSqlDialect sqlDialect = SqlDialectFactory.getSqlDialect(engineType);
            hepProgramBuilder.addRuleInstance(new FilterToGroupScanRule(FilterToGroupScanRule.DEFAULT, schema));
            RelOptPlanner relOptPlanner = new HepPlanner(hepProgramBuilder.build());
            RelToSqlConverter converter = new RelToSqlConverter(sqlDialect);
            SqlValidator sqlValidator = Configuration.getSqlValidator(
                    scope.getValidator().getCatalogReader().getRootSchema(), engineType);
            SqlToRelConverter sqlToRelConverter = Configuration.getSqlToRelConverter(scope, sqlValidator,
                    relOptPlanner, engineType);
            RelNode sqlRel = sqlToRelConverter.convertQuery(
                    sqlValidator.validate(sqlNode), false, true).rel;
            log.debug("RelNode optimize {}",
                    SemanticNode.getSql(converter.visitRoot(sqlRel).asStatement(), engineType));
            relOptPlanner.setRoot(sqlRel);
            RelNode relNode = relOptPlanner.findBestExp();
            return converter.visitRoot(relNode).asStatement();
        } catch (Exception e) {
            log.error("optimize error {}", e);
        }
        return null;
    }

    public static SqlBinaryOperator getBinaryOperator(String val) {
        if (val.equals("=")) {
            return SqlStdOperatorTable.EQUALS;
        }
        if (val.equals(">")) {
            return SqlStdOperatorTable.GREATER_THAN;
        }
        if (val.equals(">=")) {
            return SqlStdOperatorTable.GREATER_THAN_OR_EQUAL;
        }
        if (val.equals("<")) {
            return SqlStdOperatorTable.LESS_THAN;
        }
        if (val.equals("<=")) {
            return SqlStdOperatorTable.LESS_THAN_OR_EQUAL;
        }
        if (val.equals("!=")) {
            return SqlStdOperatorTable.NOT_EQUALS;
        }
        return SqlStdOperatorTable.EQUALS;
    }

    public static SqlLiteral getJoinSqlLiteral(String joinType) {
        if (Objects.nonNull(joinType) && !joinType.isEmpty()) {
            if (joinType.toLowerCase().contains(JoinType.INNER.lowerName)) {
                return SqlLiteral.createSymbol(JoinType.INNER, SqlParserPos.ZERO);
            }
            if (joinType.toLowerCase().contains(JoinType.LEFT.lowerName)) {
                return SqlLiteral.createSymbol(JoinType.LEFT, SqlParserPos.ZERO);
            }
            if (joinType.toLowerCase().contains(JoinType.RIGHT.lowerName)) {
                return SqlLiteral.createSymbol(JoinType.RIGHT, SqlParserPos.ZERO);
            }
            if (joinType.toLowerCase().contains(JoinType.FULL.lowerName)) {
                return SqlLiteral.createSymbol(JoinType.FULL, SqlParserPos.ZERO);
            }
        }
        return SqlLiteral.createSymbol(JoinType.INNER, SqlParserPos.ZERO);
    }

}
