package com.tencent.supersonic.headless.core.translator.calcite.schema;

import com.google.common.base.Preconditions;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIntervalLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.validate.SqlConformance;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * customize the  SqlDialect
 */
public class SemanticSqlDialect extends SqlDialect {

    private static final SqlConformance tagTdwSqlConformance = new SemanticSqlConformance();

    public SemanticSqlDialect(Context context) {
        super(context);
    }

    public static void unparseFetchUsingAnsi(SqlWriter writer, @Nullable SqlNode offset, @Nullable SqlNode fetch) {
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

    public void unparseSqlIntervalLiteral(SqlWriter writer, SqlIntervalLiteral literal, int leftPrec, int rightPrec) {
    }

    public void unparseOffsetFetch(SqlWriter writer, @Nullable SqlNode offset, @Nullable SqlNode fetch) {
        unparseFetchUsingAnsi(writer, offset, fetch);
    }
}
