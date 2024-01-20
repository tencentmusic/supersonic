package com.tencent.supersonic.headless.core.parser.calcite.schema;

import org.apache.calcite.sql.fun.SqlLibrary;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

/**
 * customize the  SqlConformance
 */
public class SemanticSqlConformance implements SqlConformance {

    @Override
    public boolean isLiberal() {
        return SqlConformanceEnum.BIG_QUERY.isLiberal();
    }

    @Override
    public boolean allowCharLiteralAlias() {
        return SqlConformanceEnum.BIG_QUERY.allowCharLiteralAlias();
    }

    @Override
    public boolean isGroupByAlias() {
        return SqlConformanceEnum.BIG_QUERY.isGroupByAlias();
    }

    @Override
    public boolean isGroupByOrdinal() {
        return SqlConformanceEnum.BIG_QUERY.isGroupByOrdinal();
    }

    @Override
    public boolean isHavingAlias() {
        return false;
    }

    @Override
    public boolean isSortByOrdinal() {
        return SqlConformanceEnum.BIG_QUERY.isSortByOrdinal();
    }

    @Override
    public boolean isSortByAlias() {
        return SqlConformanceEnum.BIG_QUERY.isSortByAlias();
    }

    @Override
    public boolean isSortByAliasObscures() {
        return SqlConformanceEnum.BIG_QUERY.isSortByAliasObscures();
    }

    @Override
    public boolean isFromRequired() {
        return SqlConformanceEnum.BIG_QUERY.isFromRequired();
    }

    @Override
    public boolean splitQuotedTableName() {
        return SqlConformanceEnum.BIG_QUERY.splitQuotedTableName();
    }

    @Override
    public boolean allowHyphenInUnquotedTableName() {
        return SqlConformanceEnum.BIG_QUERY.allowHyphenInUnquotedTableName();
    }

    @Override
    public boolean isBangEqualAllowed() {
        return SqlConformanceEnum.BIG_QUERY.isBangEqualAllowed();
    }

    @Override
    public boolean isPercentRemainderAllowed() {
        return SqlConformanceEnum.BIG_QUERY.isPercentRemainderAllowed();
    }

    @Override
    public boolean isMinusAllowed() {
        return SqlConformanceEnum.BIG_QUERY.isMinusAllowed();
    }

    @Override
    public boolean isApplyAllowed() {
        return SqlConformanceEnum.BIG_QUERY.isApplyAllowed();
    }

    @Override
    public boolean isInsertSubsetColumnsAllowed() {
        return SqlConformanceEnum.BIG_QUERY.isInsertSubsetColumnsAllowed();
    }

    @Override
    public boolean allowAliasUnnestItems() {
        return SqlConformanceEnum.BIG_QUERY.allowAliasUnnestItems();
    }

    @Override
    public boolean allowNiladicParentheses() {
        return SqlConformanceEnum.BIG_QUERY.allowNiladicParentheses();
    }

    @Override
    public boolean allowExplicitRowValueConstructor() {
        return SqlConformanceEnum.BIG_QUERY.allowExplicitRowValueConstructor();
    }

    @Override
    public boolean allowExtend() {
        return SqlConformanceEnum.BIG_QUERY.allowExtend();
    }

    @Override
    public boolean isLimitStartCountAllowed() {
        return true;
    }

    @Override
    public boolean isOffsetLimitAllowed() {
        return false;
    }

    @Override
    public boolean allowGeometry() {
        return SqlConformanceEnum.BIG_QUERY.allowGeometry();
    }

    @Override
    public boolean shouldConvertRaggedUnionTypesToVarying() {
        return SqlConformanceEnum.BIG_QUERY.shouldConvertRaggedUnionTypesToVarying();
    }

    @Override
    public boolean allowExtendedTrim() {
        return SqlConformanceEnum.BIG_QUERY.allowExtendedTrim();
    }

    @Override
    public boolean allowPluralTimeUnits() {
        return SqlConformanceEnum.BIG_QUERY.allowPluralTimeUnits();
    }

    @Override
    public boolean allowQualifyingCommonColumn() {
        return SqlConformanceEnum.BIG_QUERY.allowQualifyingCommonColumn();
    }

    @Override
    public boolean isValueAllowed() {
        return false;
    }

    @Override
    public SqlLibrary semantics() {
        return SqlConformanceEnum.BIG_QUERY.semantics();
    }

    @Override
    public boolean allowLenientCoercion() {
        return false;
    }
}