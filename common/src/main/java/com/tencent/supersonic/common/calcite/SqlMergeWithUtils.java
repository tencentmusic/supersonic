package com.tencent.supersonic.common.calcite;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SqlMergeWithUtils {
    public static String mergeWith(EngineType engineType, String sql, List<String> parentSqlList,
            List<String> parentWithNameList) throws SqlParseException {
        SqlParser.Config parserConfig = Configuration.getParserConfig(engineType);

        // Parse the main SQL statement
        SqlParser parser = SqlParser.create(sql, parserConfig);
        SqlNode sqlNode1 = parser.parseQuery();

        // List to hold all WITH items
        List<SqlNode> withItemList = new ArrayList<>();

        // Iterate over each parentSql and parentWithName pair
        for (int i = 0; i < parentSqlList.size(); i++) {
            String parentSql = parentSqlList.get(i);
            String parentWithName = parentWithNameList.get(i);

            // Parse the parent SQL statement
            parser = SqlParser.create(parentSql, parserConfig);
            SqlNode sqlNode2 = parser.parseQuery();

            // Create a new WITH item for parentWithName without quotes
            SqlWithItem withItem = new SqlWithItem(SqlParserPos.ZERO,
                    new SqlIdentifier(parentWithName, SqlParserPos.ZERO), null, sqlNode2,
                    SqlLiteral.createBoolean(false, SqlParserPos.ZERO));

            // Add the new WITH item to the list
            withItemList.add(withItem);
        }

        // Check if the main SQL node contains an ORDER BY or LIMIT clause
        SqlNode limitNode = null;
        SqlNodeList orderByList = null;
        if (sqlNode1 instanceof SqlOrderBy) {
            SqlOrderBy sqlOrderBy = (SqlOrderBy) sqlNode1;
            limitNode = sqlOrderBy.fetch;
            orderByList = sqlOrderBy.orderList;
            sqlNode1 = sqlOrderBy.query;
        } else if (sqlNode1 instanceof SqlSelect) {
            SqlSelect sqlSelect = (SqlSelect) sqlNode1;
            limitNode = sqlSelect.getFetch();
            sqlSelect.setFetch(null);
            sqlNode1 = sqlSelect;
        }

        // Extract existing WITH items from sqlNode1 if it is a SqlWith
        if (sqlNode1 instanceof SqlWith) {
            SqlWith sqlWith = (SqlWith) sqlNode1;
            withItemList.addAll(sqlWith.withList.getList());
            sqlNode1 = sqlWith.body;
        }

        // Create a new SqlWith node
        SqlWith finalSqlNode = new SqlWith(SqlParserPos.ZERO,
                new SqlNodeList(withItemList, SqlParserPos.ZERO), sqlNode1);

        // If there was an ORDER BY or LIMIT clause, wrap the finalSqlNode in a SqlOrderBy
        SqlNode resultNode = finalSqlNode;
        if (orderByList != null || limitNode != null) {
            resultNode = new SqlOrderBy(SqlParserPos.ZERO, finalSqlNode,
                    orderByList != null ? orderByList : SqlNodeList.EMPTY, null, limitNode);
        }

        // Custom SqlPrettyWriter configuration to avoid quoting identifiers
        SqlWriterConfig config = Configuration.getSqlWriterConfig(engineType);
        // Pretty print the final SQL
        SqlPrettyWriter writer = new SqlPrettyWriter(config);
        return writer.format(resultNode);
    }

    public static boolean hasWith(EngineType engineType, String sql) throws SqlParseException {
        SqlParser.Config parserConfig = Configuration.getParserConfig(engineType);
        SqlParser parser = SqlParser.create(sql, parserConfig);
        SqlNode sqlNode = parser.parseQuery();
        SqlNode sqlSelect = sqlNode;
        if (sqlNode instanceof SqlOrderBy) {
            SqlOrderBy sqlOrderBy = (SqlOrderBy) sqlNode;
            sqlSelect = sqlOrderBy.query;
        } else if (sqlNode instanceof SqlSelect) {
            sqlSelect = (SqlSelect) sqlNode;
        }
        return sqlSelect instanceof SqlWith;
    }
}
