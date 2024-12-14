package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This converter rewrites S2SQL including conversion from metric/dimension name to bizName and
 * build ontology query in preparation for generation of physical SQL.
 */
@Component("SqlQueryConverter")
@Slf4j
public class SqlQueryConverter implements QueryConverter {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getSqlQuery()) && queryStatement.getIsS2SQL();
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {
        convertNameToBizName(queryStatement);
        rewriteOrderBy(queryStatement);

        // fill sqlQuery
        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        String tableName = SqlSelectHelper.getTableName(sqlQuery.getSql());
        if (StringUtils.isEmpty(tableName)) {
            return;
        }
        sqlQuery.setTable(tableName.toLowerCase());
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        SemanticSchemaResp semanticSchema = queryStatement.getSemanticSchema();
        if (!sqlGenerateUtils.isSupportWith(
                EngineType.fromString(semanticSchema.getDatabaseResp().getType().toUpperCase()),
                semanticSchema.getDatabaseResp().getVersion())) {
            sqlQuery.setSupportWith(false);
            sqlQuery.setWithAlias(false);
        }

        // build ontologyQuery
        List<String> allQueryFields = SqlSelectHelper.getAllSelectFields(sqlQuery.getSql());
        List<MetricSchemaResp> queryMetrics = semanticSchema.getMetrics(allQueryFields);
        List<DimSchemaResp> queryDimensions = semanticSchema.getDimensions(allQueryFields);
        OntologyQuery ontologyQuery = new OntologyQuery();
        ontologyQuery.getMetrics().addAll(queryMetrics);
        ontologyQuery.getDimensions().addAll(queryDimensions);

        AggOption sqlQueryAggOption = getAggOption(sqlQuery.getSql(), queryMetrics);
        // if sql query itself has aggregation, ontology query just returns detail
        if (sqlQueryAggOption.equals(AggOption.AGGREGATION)) {
            ontologyQuery.setAggOption(AggOption.NATIVE);
        } else if (sqlQueryAggOption.equals(AggOption.NATIVE) && !queryMetrics.isEmpty()) {
            ontologyQuery.setAggOption(AggOption.DEFAULT);
        }
        ontologyQuery.setNativeQuery(!AggOption.isAgg(ontologyQuery.getAggOption()));

        queryStatement.setOntologyQuery(ontologyQuery);
        log.info("parse sqlQuery [{}] ", sqlQuery);
    }

    private AggOption getAggOption(String sql, List<MetricSchemaResp> metricSchemas) {
        if (SqlSelectFunctionHelper.hasAggregateFunction(sql)) {
            return AggOption.AGGREGATION;
        }

        if (!SqlSelectFunctionHelper.hasAggregateFunction(sql) && !SqlSelectHelper.hasGroupBy(sql)
                && !SqlSelectHelper.hasWith(sql) && !SqlSelectHelper.hasSubSelect(sql)) {
            log.debug("getAggOption simple sql set to DEFAULT");
            return AggOption.NATIVE;
        }

        // if there is no group by in S2SQL,set MetricTable's aggOption to "NATIVE"
        // if there is count() in S2SQL,set MetricTable's aggOption to "NATIVE"
        if (!SqlSelectFunctionHelper.hasAggregateFunction(sql)
                || SqlSelectFunctionHelper.hasFunction(sql, "count")
                || SqlSelectFunctionHelper.hasFunction(sql, "count_distinct")) {
            return AggOption.OUTER;
        }

        if (SqlSelectHelper.hasSubSelect(sql) || SqlSelectHelper.hasWith(sql)
                || SqlSelectHelper.hasGroupBy(sql)) {
            return AggOption.OUTER;
        }
        long defaultAggNullCnt = metricSchemas.stream().filter(
                m -> Objects.isNull(m.getDefaultAgg()) || StringUtils.isBlank(m.getDefaultAgg()))
                .count();
        if (defaultAggNullCnt > 0) {
            log.debug("getAggOption find null defaultAgg metric set to NATIVE");
            return AggOption.DEFAULT;
        }
        return AggOption.DEFAULT;
    }

    private void convertNameToBizName(QueryStatement queryStatement) {
        SemanticSchemaResp semanticSchema = queryStatement.getSemanticSchema();
        Map<String, String> fieldNameToBizNameMap = semanticSchema.getNameToBizNameMap();
        String sql = queryStatement.getSqlQuery().getSql();
        log.debug("dataSetId:{},convert name to bizName before:{}", queryStatement.getDataSetId(),
                sql);
        sql = SqlReplaceHelper.replaceFields(sql, fieldNameToBizNameMap, true);
        log.debug("dataSetId:{},convert name to bizName after:{}", queryStatement.getDataSetId(),
                sql);
        sql = SqlReplaceHelper.replaceTable(sql,
                Constants.TABLE_PREFIX + queryStatement.getDataSetId());
        log.debug("replaceTableName after:{}", sql);
        queryStatement.getSqlQuery().setSql(sql);
    }

    private void rewriteOrderBy(QueryStatement queryStatement) {
        // replace order by field with the select sequence number
        String sql = queryStatement.getSqlQuery().getSql();
        String newSql = SqlReplaceHelper.replaceAggAliasOrderbyField(sql);
        log.debug("replaceOrderAggSameAlias {} -> {}", sql, newSql);
        queryStatement.getSqlQuery().setSql(newSql);
    }

}
