package com.tencent.supersonic.semantic.query.application.parser.convert;

import static com.tencent.supersonic.common.constant.Constants.UNDERLINE;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.query.pojo.Param;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.core.domain.Catalog;
import com.tencent.supersonic.semantic.query.application.parser.SemanticConverter;
import com.tencent.supersonic.semantic.query.domain.utils.QueryStructUtils;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component("ParserDefaultConverter")
@Slf4j
public class ParserDefaultConverter implements SemanticConverter {

    @Value("${internal.metric.cnt.suffix:internal_cnt}")
    private String internalMetricNameSuffix;

    private final CalculateConverterAgg calculateCoverterAgg;
    private final QueryStructUtils queryStructUtils;

    public ParserDefaultConverter(
            CalculateConverterAgg calculateCoverterAgg,
            QueryStructUtils queryStructUtils) {
        this.calculateCoverterAgg = calculateCoverterAgg;
        this.queryStructUtils = queryStructUtils;
    }

    @Override
    public boolean accept(QueryStructReq queryStructCmd) {
        return !calculateCoverterAgg.accept(queryStructCmd);
    }

    @Override
    public void converter(Catalog catalog, QueryStructReq queryStructCmd, ParseSqlReq sqlCommend,
            MetricReq metricCommand)
            throws Exception {
        MetricReq metricReq = generateSqlCommand(catalog, queryStructCmd);
        BeanUtils.copyProperties(metricReq, metricCommand);
    }


    public MetricReq generateSqlCommand(Catalog catalog, QueryStructReq queryStructCmd) {
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
        String rootPath = catalog.getDomainFullPath(queryStructCmd.getDomainId());
        sqlCommend.setRootPath(rootPath);

        // todo tmp delete
        // support detail query
        if (queryStructCmd.getNativeQuery() && CollectionUtils.isEmpty(sqlCommend.getMetrics())) {
            String internalMetricName = generateInternalMetricName(catalog, queryStructCmd);
            sqlCommend.getMetrics().add(internalMetricName);
        }

        return sqlCommend;
    }


    public String generateInternalMetricName(Catalog catalog, QueryStructReq queryStructCmd) {
        String internalMetricNamePrefix = "";
        if (CollectionUtils.isEmpty(queryStructCmd.getGroups())) {
            log.warn("group is empty!");
        } else {
            String group = queryStructCmd.getGroups().get(0).equalsIgnoreCase("sys_imp_date")
                    ? queryStructCmd.getGroups().get(1) : queryStructCmd.getGroups().get(0);
            DimensionResp dimension = catalog.getDimension(group, queryStructCmd.getDomainId());
            String datasourceBizName = dimension.getDatasourceBizName();
            if (Strings.isNotEmpty(datasourceBizName)) {
                internalMetricNamePrefix = datasourceBizName + UNDERLINE;
            }

        }
        String internalMetricName = internalMetricNamePrefix + internalMetricNameSuffix;
        return internalMetricName;
    }
}
