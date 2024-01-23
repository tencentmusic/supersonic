package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SemanticSqlDialect;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlDialect.Context;
import org.apache.calcite.sql.SqlDialect.DatabaseProduct;


public class SqlDialectFactory {

    public static final Context DEFAULT_CONTEXT = SqlDialect.EMPTY_CONTEXT
            .withDatabaseProduct(DatabaseProduct.BIG_QUERY)
            .withLiteralQuoteString("'")
            .withLiteralEscapedQuoteString("''")
            .withIdentifierQuoteString("`")
            .withUnquotedCasing(Casing.UNCHANGED)
            .withQuotedCasing(Casing.UNCHANGED)
            .withCaseSensitive(false);
    public static final Context POSTGRESQL_CONTEXT = SqlDialect.EMPTY_CONTEXT
            .withDatabaseProduct(DatabaseProduct.BIG_QUERY)
            .withLiteralQuoteString("'")
            .withLiteralEscapedQuoteString("''")
            .withUnquotedCasing(Casing.UNCHANGED)
            .withQuotedCasing(Casing.UNCHANGED)
            .withCaseSensitive(false);
    private static Map<EngineType, SemanticSqlDialect> sqlDialectMap;

    static {
        sqlDialectMap = new HashMap<>();
        sqlDialectMap.put(EngineType.CLICKHOUSE, new SemanticSqlDialect(DEFAULT_CONTEXT));
        sqlDialectMap.put(EngineType.MYSQL, new SemanticSqlDialect(DEFAULT_CONTEXT));
        sqlDialectMap.put(EngineType.H2, new SemanticSqlDialect(DEFAULT_CONTEXT));
        sqlDialectMap.put(EngineType.POSTGRESQL, new SemanticSqlDialect(POSTGRESQL_CONTEXT));
    }

    public static SemanticSqlDialect getSqlDialect(EngineType engineType) {
        SemanticSqlDialect semanticSqlDialect = sqlDialectMap.get(engineType);
        if (Objects.isNull(semanticSqlDialect)) {
            return new SemanticSqlDialect(DEFAULT_CONTEXT);
        }
        return semanticSqlDialect;
    }
}
