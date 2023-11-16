package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

/**
 * Sql Parser equal Helper
 */
@Slf4j
public class SqlParserEqualHelper {

    /**
     * determine if two SQL statements are equal.
     *
     * @param thisSql
     * @param otherSql
     * @return
     */
    public static boolean equals(String thisSql, String otherSql) {
        //1. select fields
        List<String> thisSelectFields = SqlParserSelectHelper.getSelectFields(thisSql);
        List<String> otherSelectFields = SqlParserSelectHelper.getSelectFields(otherSql);

        if (!CollectionUtils.isEqualCollection(thisSelectFields, otherSelectFields)) {
            return false;
        }

        //2. all fields
        List<String> thisAllFields = SqlParserSelectHelper.getAllFields(thisSql);
        List<String> otherAllFields = SqlParserSelectHelper.getAllFields(otherSql);

        if (!CollectionUtils.isEqualCollection(thisAllFields, otherAllFields)) {
            return false;
        }

        //3. where
        List<FieldExpression> thisFieldExpressions = SqlParserSelectHelper.getFilterExpression(thisSql);
        List<FieldExpression> otherFieldExpressions = SqlParserSelectHelper.getFilterExpression(otherSql);

        if (!CollectionUtils.isEqualCollection(thisFieldExpressions, otherFieldExpressions)) {
            return false;
        }
        //4. tableName
        if (!SqlParserSelectHelper.getDbTableName(thisSql)
                .equalsIgnoreCase(SqlParserSelectHelper.getDbTableName(otherSql))) {
            return false;
        }
        //5. having
        List<FieldExpression> thisHavingExpressions = SqlParserSelectHelper.getHavingExpressions(thisSql);
        List<FieldExpression> otherHavingExpressions = SqlParserSelectHelper.getHavingExpressions(otherSql);

        if (!CollectionUtils.isEqualCollection(thisHavingExpressions, otherHavingExpressions)) {
            return false;
        }
        //6. orderBy
        List<FieldExpression> thisOrderByExpressions = SqlParserSelectHelper.getOrderByExpressions(thisSql);
        List<FieldExpression> otherOrderByExpressions = SqlParserSelectHelper.getOrderByExpressions(otherSql);

        if (!CollectionUtils.isEqualCollection(thisOrderByExpressions, otherOrderByExpressions)) {
            return false;
        }
        return true;
    }

}

