package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQueryParam;
import com.tencent.supersonic.headless.core.pojo.StructQueryParam;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.OntologyQueryParam;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component("ParserDefaultConverter")
@Slf4j
public class StructQueryConverter implements QueryConverter {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        if (Objects.nonNull(queryStatement.getStructQueryParam()) && !queryStatement.getIsS2SQL()) {
            return true;
        }

        return false;
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        StructQueryParam structQueryParam = queryStatement.getStructQueryParam();

        String dsTable = "t_1";
        SqlQueryParam sqlParam = new SqlQueryParam();
        sqlParam.setTable(dsTable);
        String sql = String.format("select %s from %s  %s %s %s",
                sqlGenerateUtils.getSelect(structQueryParam), dsTable,
                sqlGenerateUtils.getGroupBy(structQueryParam),
                sqlGenerateUtils.getOrderBy(structQueryParam),
                sqlGenerateUtils.getLimit(structQueryParam));
        Database database = queryStatement.getOntology().getDatabase();
        EngineType engineType = EngineType.fromString(database.getType().toUpperCase());
        if (!sqlGenerateUtils.isSupportWith(engineType, database.getVersion())) {
            sqlParam.setSupportWith(false);
            sql = String.format("select %s from %s t0 %s %s %s",
                    sqlGenerateUtils.getSelect(structQueryParam), dsTable,
                    sqlGenerateUtils.getGroupBy(structQueryParam),
                    sqlGenerateUtils.getOrderBy(structQueryParam),
                    sqlGenerateUtils.getLimit(structQueryParam));
        }
        sqlParam.setSql(sql);
        queryStatement.setSqlQueryParam(sqlParam);

        OntologyQueryParam ontologyQueryParam = new OntologyQueryParam();
        ontologyQueryParam.getDimensions().addAll(structQueryParam.getGroups());
        ontologyQueryParam.getMetrics().addAll(structQueryParam.getAggregators().stream()
                .map(a -> a.getColumn()).collect(Collectors.toList()));
        String where = sqlGenerateUtils.generateWhere(structQueryParam, null);
        ontologyQueryParam.setWhere(where);
        ontologyQueryParam.setAggOption(AggOption.AGGREGATION);
        ontologyQueryParam.setNativeQuery(structQueryParam.getQueryType().isNativeAggQuery());
        ontologyQueryParam.setOrder(structQueryParam.getOrders().stream()
                .map(order -> new ColumnOrder(order.getColumn(), order.getDirection()))
                .collect(Collectors.toList()));
        ontologyQueryParam.setLimit(structQueryParam.getLimit());
        queryStatement.setOntologyQueryParam(ontologyQueryParam);
        log.info("parse structQuery [{}] ", queryStatement.getSqlQueryParam());
    }

}
