package com.tencent.supersonic.semantic.query.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.UNDERLINE;

import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.pojo.Param;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.query.domain.ParserService;
import com.tencent.supersonic.semantic.query.domain.utils.calculate.CalculateConverter;
import com.tencent.supersonic.semantic.query.domain.utils.calculate.CalculateConverterAgg;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class ParserCommandConverter {

    private final ParserService parserService;
    private final DomainService domainService;
    private final CalculateConverterAgg calculateCoverterAgg;
    @Value("${internal.metric.cnt.suffix:internal_cnt}")
    private String internalMetricNameSuffix;
    private final DimensionService dimensionService;

    private List<CalculateConverter> calculateCoverters = new LinkedList<>();
    private final QueryStructUtils queryStructUtils;

    public ParserCommandConverter(ParserService parserService,
            DomainService domainService,
            CalculateConverterAgg calculateCoverterAgg,
            DimensionService dimensionService,
            @Lazy QueryStructUtils queryStructUtils) {
        this.parserService = parserService;
        this.domainService = domainService;
        this.calculateCoverterAgg = calculateCoverterAgg;
        this.dimensionService = dimensionService;
        this.queryStructUtils = queryStructUtils;
        calculateCoverters.add(calculateCoverterAgg);

    }

    public SqlParserResp getSqlParser(QueryStructReq queryStructCmd) throws Exception {
        StatUtils.get().setUseSqlCache(false);
        for (CalculateConverter calculateConverter : calculateCoverters) {
            if (calculateConverter.accept(queryStructCmd)) {
                log.info("getSqlParser {}", calculateConverter.getClass());
                return calculateConverter.getSqlParser(queryStructCmd);
            }
        }
        return parserService.physicalSql(generateSqlCommand(queryStructCmd));
    }


    public MetricReq generateSqlCommand(QueryStructReq queryStructCmd) {
        MetricReq sqlCommend = new MetricReq();
        sqlCommend.setMetrics(queryStructCmd.getMetrics());
        sqlCommend.setDimensions(queryStructCmd.getGroups());
        String where = queryStructUtils.generateWhere(queryStructCmd);
        log.info("in generateSqlCommend, complete where:{}", where);

        sqlCommend.setWhere(where);
        sqlCommend.setOrder(queryStructCmd.getOrders().stream()
                .map(order -> new ColumnOrder(order.getColumn(), order.getDirection())).collect(Collectors.toList()));
        sqlCommend.setVariables(queryStructCmd.getParams().stream()
                .collect(Collectors.toMap(Param::getName, Param::getValue, (k1, k2) -> k1)));
        sqlCommend.setLimit(queryStructCmd.getLimit());
        String rootPath = domainService.getDomainFullPath(queryStructCmd.getDomainId());
        sqlCommend.setRootPath(rootPath);
        // support detail query
        if (queryStructCmd.getNativeQuery() && CollectionUtils.isEmpty(sqlCommend.getMetrics())) {
            String internalMetricName = generateInternalMetricName(queryStructCmd);
            sqlCommend.getMetrics().add(internalMetricName);
        }
        return sqlCommend;
    }


    public String generateInternalMetricName(QueryStructReq queryStructCmd) {
        String internalMetricNamePrefix = "";
        if (CollectionUtils.isEmpty(queryStructCmd.getGroups())) {
            log.warn("group is empty!");
        } else {
            String group = queryStructCmd.getGroups().get(0).equalsIgnoreCase("sys_imp_date")
                    ? queryStructCmd.getGroups().get(1) : queryStructCmd.getGroups().get(0);
            DimensionResp dimension = dimensionService.getDimension(group, queryStructCmd.getDomainId());
            String datasourceBizName = dimension.getDatasourceBizName();
            if (Strings.isNotEmpty(datasourceBizName)) {
                internalMetricNamePrefix = datasourceBizName + UNDERLINE;
            }

        }
        String internalMetricName = internalMetricNamePrefix + internalMetricNameSuffix;
        return internalMetricName;
    }
}
