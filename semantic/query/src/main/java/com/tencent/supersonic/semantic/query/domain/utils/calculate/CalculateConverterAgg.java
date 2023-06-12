package com.tencent.supersonic.semantic.query.domain.utils.calculate;


import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.query.domain.ParserService;
import com.tencent.supersonic.semantic.query.domain.utils.QueryStructUtils;
import com.tencent.supersonic.semantic.query.domain.utils.SqlGenerateUtils;
import java.util.ArrayList;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class CalculateConverterAgg implements CalculateConverter {

    private final DomainService domainService;
    private final ParserService parserService;
    private final CalculateConverterRatio calculateCoverterRatio;
    private final QueryStructUtils queryStructUtils;
    private final SqlGenerateUtils sqlGenerateUtils;

    @Value("${metricParser.agg.default:sum}")
    private String metricAggDefault;


    public CalculateConverterAgg(DomainService domainService,
            ParserService parserService,
            CalculateConverterRatio calculateCoverterRatio,
            @Lazy QueryStructUtils queryStructUtils,
            SqlGenerateUtils sqlGenerateUtils) {
        this.domainService = domainService;
        this.parserService = parserService;
        this.calculateCoverterRatio = calculateCoverterRatio;
        this.queryStructUtils = queryStructUtils;
        this.sqlGenerateUtils = sqlGenerateUtils;
    }

    public ParseSqlReq generateSqlCommend(QueryStructReq queryStructCmd) throws Exception {
        // 同环比
        if (calculateCoverterRatio.accept(queryStructCmd)) {
            return calculateCoverterRatio.generateSqlCommand(queryStructCmd);
        }
        ParseSqlReq sqlCommand = new ParseSqlReq();
        sqlCommand.setRootPath(domainService.getDomainFullPath(queryStructCmd.getDomainId()));
        String metricTableName = "metric_tb";
        MetricTable metricTable = new MetricTable();
        metricTable.setAlias(metricTableName);
        metricTable.setMetrics(queryStructCmd.getMetrics());
        metricTable.setDimensions(queryStructCmd.getGroups());
        String where = queryStructUtils.generateWhere(queryStructCmd);
        log.info("in generateSqlCommand, complete where:{}", where);
        metricTable.setWhere(where);
        metricTable.setAgg(true);
        sqlCommand.setTables(new ArrayList<>(Collections.singletonList(metricTable)));
        String sql = String.format("select %s from %s  %s %s %s", sqlGenerateUtils.getSelect(queryStructCmd),
                metricTableName,
                sqlGenerateUtils.getGroupBy(queryStructCmd), sqlGenerateUtils.getOrderBy(queryStructCmd),
                sqlGenerateUtils.getLimit(queryStructCmd));
        sqlCommand.setSql(sql);
        return sqlCommand;
    }


    @Override
    public boolean accept(QueryStructReq queryStructCmd) {
        if (queryStructCmd.getNativeQuery()) {
            return false;
        }
        if (CollectionUtils.isEmpty(queryStructCmd.getAggregators())) {
            return false;
        }
        //todo ck类型暂不拼with语句
        if (queryStructCmd.getDomainId().equals(34L)) {
            return false;
        }
        int nonSumFunction = 0;
        for (Aggregator agg : queryStructCmd.getAggregators()) {
            if (agg.getFunc() == null || "".equals(agg.getFunc())) {
                return false;
            }
            if (agg.getFunc().equals(AggOperatorEnum.UNKNOWN)) {
                return false;
            }
            if (agg.getFunc() != null
                // && !agg.getFunc().equalsIgnoreCase(MetricAggDefault)
            ) {
                nonSumFunction++;
            }
        }
        return nonSumFunction > 0;
    }

    @Override
    public SqlParserResp getSqlParser(QueryStructReq queryStructCmd) throws Exception {
        return parserService.physicalSql(generateSqlCommend(queryStructCmd));
    }

}
