package com.tencent.supersonic.headless.core.translator.parser;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * This parser appends default dimension values (if configured) to the where statement.
 */
@Slf4j
@Component("DefaultDimValueParser")
public class DefaultDimValueParser implements QueryParser {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getSqlQuery())
                && StringUtils.isNotBlank(queryStatement.getSqlQuery().getSql())
                && !CollectionUtils.isEmpty(queryStatement.getOntology().getDimensions());
    }

    @Override
    public void parse(QueryStatement queryStatement) {
        List<DimSchemaResp> dimensions = queryStatement.getOntology().getDimensions().stream()
                .filter(dimension -> !CollectionUtils.isEmpty(dimension.getDefaultValues()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        String sql = queryStatement.getSqlQuery().getSql();
        List<String> whereFields =
                SqlSelectHelper.getWhereFields(sql).stream().collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(whereFields)) {
            return;
        }
        List<Expression> expressions = Lists.newArrayList();
        for (DimSchemaResp dimension : dimensions) {
            ExpressionList expressionList = new ExpressionList();
            List<Expression> exprs = new ArrayList<>();
            dimension.getDefaultValues().forEach(value -> exprs.add(new StringValue(value)));
            expressionList.setExpressions(exprs);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column(dimension.getBizName()));
            inExpression.setRightExpression(expressionList);
            expressions.add(inExpression);
            if (Objects.nonNull(queryStatement.getSqlQuery().getTable())) {
                queryStatement.getOntologyQuery().getDimensions().add(dimension);
            }
        }
        sql = SqlAddHelper.addWhere(sql, expressions);
        queryStatement.getSqlQuery().setSql(sql);
    }
}
