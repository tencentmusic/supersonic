package com.tencent.supersonic.semantic.query.domain.calcite;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.query.optimizer.QueryOptimizer;
import com.tencent.supersonic.semantic.query.parser.QueryParser;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.utils.ComponentFactory;
import com.tencent.supersonic.semantic.query.utils.QueryUtils;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

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
            QueryStatement queryStatement = queryParser.logicSql(queryStructReq);
            queryUtils.checkSqlParse(queryStatement);
            queryStatement.setModelId(queryStructReq.getModelId());
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
