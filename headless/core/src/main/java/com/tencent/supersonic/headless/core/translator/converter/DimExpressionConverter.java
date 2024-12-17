package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.Dimension;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * This converter replaces dimension bizName in the S2SQL with calculation expression (if
 * configured).
 */
@Component("DimExpressionConverter")
@Slf4j
public class DimExpressionConverter implements QueryConverter {
    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getSqlQuery())
                && StringUtils.isNotBlank(queryStatement.getSqlQuery().getSql());
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {

        SemanticSchemaResp semanticSchema = queryStatement.getSemanticSchema();
        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        OntologyQuery ontologyQuery = queryStatement.getOntologyQuery();

        Map<String, String> bizName2Expr = getDimensionExpressions(semanticSchema, ontologyQuery);
        if (!CollectionUtils.isEmpty(bizName2Expr)) {
            String sql = SqlReplaceHelper.replaceSqlByExpression(sqlQuery.getSql(), bizName2Expr);
            sqlQuery.setSql(sql);
        }
    }

    private Map<String, String> getDimensionExpressions(SemanticSchemaResp semanticSchema,
            OntologyQuery ontologyQuery) {

        Set<DimSchemaResp> queryDimensions = ontologyQuery.getDimensions();
        Set<String> queryFields = ontologyQuery.getFields();
        log.debug("begin to generateDerivedMetric {} [{}]", queryDimensions);

        Set<String> allFields = new HashSet<>();
        Map<String, Dimension> dimensionMap = new HashMap<>();
        semanticSchema.getModelResps().forEach(modelResp -> {
            allFields.addAll(modelResp.getFieldList());
            if (modelResp.getModelDetail().getDimensions() != null) {
                modelResp.getModelDetail().getDimensions()
                        .forEach(dimension -> dimensionMap.put(dimension.getBizName(), dimension));
            }
        });


        Map<String, String> dim2Expr = new HashMap<>();
        for (DimSchemaResp queryDim : queryDimensions) {
            queryDim.getFields().addAll(SqlSelectHelper.getFieldsFromExpr(queryDim.getExpr()));
            dim2Expr.put(queryDim.getBizName(), queryDim.getExpr());
            queryFields.addAll(queryDim.getFields());
        }

        return dim2Expr;
    }

}
