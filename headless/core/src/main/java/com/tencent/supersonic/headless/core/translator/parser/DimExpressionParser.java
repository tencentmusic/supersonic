package com.tencent.supersonic.headless.core.translator.parser;

import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This parser replaces dimension bizName in the S2SQL with calculation expression (if configured).
 */
@Component("DimExpressionParser")
@Slf4j
public class DimExpressionParser implements QueryParser {
    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getSqlQuery())
                && Objects.nonNull(queryStatement.getOntologyQuery())
                && StringUtils.isNotBlank(queryStatement.getSqlQuery().getSql())
                && !CollectionUtils.isEmpty(queryStatement.getOntologyQuery().getDimensions());
    }

    @Override
    public void parse(QueryStatement queryStatement) throws Exception {

        SemanticSchemaResp semanticSchema = queryStatement.getSemanticSchema();
        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        OntologyQuery ontologyQuery = queryStatement.getOntologyQuery();

        Map<String, String> bizName2Expr = getDimensionExpressions(semanticSchema, ontologyQuery);
        if (!CollectionUtils.isEmpty(bizName2Expr)) {
            String sql = SqlReplaceHelper.replaceSqlByExpression(sqlQuery.getTable(),
                    sqlQuery.getSql(), bizName2Expr);
            sqlQuery.setSql(sql);
        }
    }

    private Map<String, String> getDimensionExpressions(SemanticSchemaResp semanticSchema,
            OntologyQuery ontologyQuery) {

        Set<DimSchemaResp> queryDimensions = ontologyQuery.getDimensions();
        Set<String> queryFields = ontologyQuery.getFields();
        log.debug("begin to generateDerivedMetric {} [{}]", queryDimensions);

        Map<String, String> dim2Expr = new HashMap<>();
        for (DimSchemaResp queryDim : queryDimensions) {
            queryDim.getFields().addAll(SqlSelectHelper.getFieldsFromExpr(queryDim.getExpr()));
            queryFields.addAll(queryDim.getFields());
            if (!queryDim.getBizName().equals(queryDim.getExpr())) {
                dim2Expr.put(queryDim.getBizName(), queryDim.getExpr());
            }
        }

        return dim2Expr;
    }

}
