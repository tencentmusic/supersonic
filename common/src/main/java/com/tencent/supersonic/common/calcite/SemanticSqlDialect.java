package com.tencent.supersonic.common.calcite;

import com.google.common.base.Preconditions;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlMonotonicBinaryOperator;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlConformance;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * customize the SqlDialect
 */
public class SemanticSqlDialect extends SqlDialect {

    private static final SqlConformance tagTdwSqlConformance = new SemanticSqlConformance();

    public SemanticSqlDialect(Context context) {
        super(context);
    }

    public static void unparseFetchUsingAnsi(SqlWriter writer, @Nullable SqlNode offset,
            @Nullable SqlNode fetch) {
        Preconditions.checkArgument(fetch != null || offset != null);
        SqlWriter.Frame fetchFrame;
        writer.newlineAndIndent();
        fetchFrame = writer.startList(SqlWriter.FrameTypeEnum.OFFSET);
        writer.keyword("LIMIT");
        boolean hasOffset = false;
        if (offset != null) {
            offset.unparse(writer, -1, -1);
            hasOffset = true;
        }

        if (fetch != null) {
            if (hasOffset) {
                writer.keyword(",");
            }
            fetch.unparse(writer, -1, -1);
        }

        writer.endList(fetchFrame);
    }

    @Override
    public void quoteStringLiteralUnicode(StringBuilder buf, String val) {
        buf.append("'");
        buf.append(val);
        buf.append("'");
    }

    @Override
    public void quoteStringLiteral(StringBuilder buf, String charsetName, String val) {
        buf.append(literalQuoteString);
        buf.append(val.replace(literalEndQuoteString, literalEscapedQuote));
        buf.append(literalEndQuoteString);
    }

    @Override
    public boolean supportsCharSet() {
        return false;
    }

    @Override
    public boolean requiresAliasForFromItems() {
        return true;
    }

    @Override
    public SqlConformance getConformance() {
        // mysql_5
        return tagTdwSqlConformance;
    }

    public boolean supportsGroupByWithCube() {
        return true;
    }

    public void unparseSqlIntervalLiteral(SqlWriter writer, SqlIntervalLiteral literal,
            int leftPrec, int rightPrec) {}

    public void unparseOffsetFetch(SqlWriter writer, @Nullable SqlNode offset,
            @Nullable SqlNode fetch) {
        unparseFetchUsingAnsi(writer, offset, fetch);
    }

    public boolean supportsNestedAggregations() {
        return false;
    }


    public void unparseCall(SqlWriter writer, SqlCall call, int leftPrec, int rightPrec) {
        if (modifyIntervalTime(call, writer, leftPrec, rightPrec)) {
            return;
        }
        super.unparseCall(writer, call, leftPrec, rightPrec);

    }

    private Boolean modifyIntervalTime(SqlCall call, SqlWriter writer, int leftPrec,
            int rightPrec) {
        SqlOperator operator = call.getOperator();
        if (operator instanceof SqlMonotonicBinaryOperator
                && call.getKind().equals(SqlKind.TIMES)) {
            if (call.getOperandList() != null && call.getOperandList().size() == 2
                    && call.getOperandList().get(1) instanceof SqlIntervalLiteral) {
                SqlIntervalLiteral intervalOperand =
                        (SqlIntervalLiteral) call.getOperandList().get(1);
                SqlIntervalLiteral.IntervalValue interval =
                        (SqlIntervalLiteral.IntervalValue) intervalOperand.getValue();
                call.setOperand(1, SqlNumericLiteral.createExactNumeric(interval.toString(),
                        SqlParserPos.ZERO));
                writer.keyword(SqlKind.INTERVAL.name());
                call.unparse(writer, leftPrec, rightPrec);
                unparseSqlIntervalQualifier(writer, interval.getIntervalQualifier(),
                        RelDataTypeSystem.DEFAULT);
                return true;
            }
        }
        return false;
    }
}
