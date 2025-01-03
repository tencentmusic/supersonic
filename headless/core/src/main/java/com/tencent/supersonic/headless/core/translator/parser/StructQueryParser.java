package com.tencent.supersonic.headless.core.translator.parser;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import com.tencent.supersonic.headless.core.pojo.StructQuery;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component("StructQueryParser")
@Slf4j
public class StructQueryParser implements QueryParser {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getStructQuery()) && !queryStatement.getIsS2SQL();
    }

    @Override
    public void parse(QueryStatement queryStatement) throws Exception {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        StructQuery structQuery = queryStatement.getStructQuery();

        String dsTable = "t_1";
        SqlQuery sqlParam = new SqlQuery();
        sqlParam.setTable(dsTable);
        String sql = String.format("select %s from %s  %s %s %s",
                sqlGenerateUtils.getSelect(structQuery), dsTable,
                sqlGenerateUtils.getGroupBy(structQuery), sqlGenerateUtils.getOrderBy(structQuery),
                sqlGenerateUtils.getLimit(structQuery));
        if (!sqlGenerateUtils.isSupportWith(queryStatement.getOntology().getDatabaseType(),
                queryStatement.getOntology().getDatabaseVersion())) {
            sqlParam.setSupportWith(false);
            sql = String.format("select %s from %s t0 %s %s %s",
                    sqlGenerateUtils.getSelect(structQuery), dsTable,
                    sqlGenerateUtils.getGroupBy(structQuery),
                    sqlGenerateUtils.getOrderBy(structQuery),
                    sqlGenerateUtils.getLimit(structQuery));
        }
        sqlParam.setSql(sql);
        queryStatement.setSqlQuery(sqlParam);

        OntologyQuery ontologyQuery = new OntologyQuery();
        ontologyQuery.getDimensions().addAll(structQuery.getGroups());
        ontologyQuery.getMetrics().addAll(structQuery.getAggregators().stream()
                .map(Aggregator::getColumn).collect(Collectors.toList()));
        String where = sqlGenerateUtils.generateWhere(structQuery, null);
        ontologyQuery.setWhere(where);
        if (ontologyQuery.getMetrics().isEmpty()) {
            ontologyQuery.setAggOption(AggOption.NATIVE);
        } else {
            ontologyQuery.setAggOption(AggOption.DEFAULT);
        }
        ontologyQuery.setNativeQuery(structQuery.getQueryType().isNativeAggQuery());
        ontologyQuery.setOrder(structQuery.getOrders().stream()
                .map(order -> new ColumnOrder(order.getColumn(), order.getDirection()))
                .collect(Collectors.toList()));
        ontologyQuery.setLimit(structQuery.getLimit());
        queryStatement.setOntologyQuery(ontologyQuery);
        log.info("parse structQuery [{}] ", queryStatement.getSqlQuery());
    }

}
