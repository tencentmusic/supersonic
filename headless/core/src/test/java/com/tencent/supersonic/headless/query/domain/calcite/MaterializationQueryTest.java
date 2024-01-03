package com.tencent.supersonic.headless.query.domain.calcite;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.optimizer.QueryOptimizer;
import com.tencent.supersonic.headless.core.parser.QueryParser;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.core.utils.QueryUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class MaterializationQueryTest {

    private final QueryParser queryParser;
    private final QueryUtils queryUtils;

    public MaterializationQueryTest(QueryParser queryParser,
            QueryUtils queryUtils) {
        this.queryParser = queryParser;
        this.queryUtils = queryUtils;
    }

    public void test() {
        QueryStructReq queryStructReq = new QueryStructReq();
        queryStructReq.setModelId(1L);

        Aggregator aggregator = new Aggregator();
        aggregator.setFunc(AggOperatorEnum.UNKNOWN);
        aggregator.setColumn("pv");
        queryStructReq.setAggregators(Arrays.asList(aggregator));

        queryStructReq.setGroups(Arrays.asList("department"));
        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateMode.LIST);
        dateConf.setDateList(Arrays.asList("2023-08-01"));
        queryStructReq.setDateInfo(dateConf);

        try {
            QueryStatement queryStatement = new QueryStatement();
            queryStatement.setQueryStructReq(queryStructReq);
            queryStatement.setIsS2SQL(false);
            queryStatement = queryParser.logicSql(queryStatement);
            queryUtils.checkSqlParse(queryStatement);
            queryStatement.setModelIds(queryStructReq.getModelIds());
            log.info("queryStatement:{}", queryStatement);
            for (QueryOptimizer queryOptimizer : ComponentFactory.getQueryOptimizers()) {
                queryOptimizer.rewrite(queryStructReq, queryStatement);
            }
            //queryParser.test(queryStructReq,metricReq);
            log.info("queryStatement:{}", queryStatement);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
