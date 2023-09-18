package com.tencent.supersonic.semantic.query.utils;

import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE;

import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.ItemDateResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.query.request.QueryDslReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Slf4j
@Component
public class QueryStructUtils {
    public static Set<String> internalCols = new HashSet<>(
            Arrays.asList("dayno", "plat_sys_var", "sys_imp_date", "sys_imp_week", "sys_imp_month"));
    private final DateUtils dateUtils;
    private final SqlFilterUtils sqlFilterUtils;
    private final Catalog catalog;
    @Value("${internal.metric.cnt.suffix:internal_cnt}")
    private String internalMetricNameSuffix;

    public QueryStructUtils(
            DateUtils dateUtils,
            SqlFilterUtils sqlFilterUtils, Catalog catalog) {

        this.dateUtils = dateUtils;
        this.sqlFilterUtils = sqlFilterUtils;
        this.catalog = catalog;
    }


    private List<Long> getDimensionIds(QueryStructReq queryStructCmd) {
        List<Long> dimensionIds = new ArrayList<>();
        List<DimensionResp> dimensions = catalog.getDimensions(queryStructCmd.getModelId());
        Map<String, List<DimensionResp>> pair = dimensions.stream()
                .collect(Collectors.groupingBy(DimensionResp::getBizName));
        for (String group : queryStructCmd.getGroups()) {
            if (pair.containsKey(group)) {
                dimensionIds.add(pair.get(group).get(0).getId());
            }
        }

        List<String> filtersCols = sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter());
        for (String col : filtersCols) {
            if (pair.containsKey(col)) {
                dimensionIds.add(pair.get(col).get(0).getId());
            }
        }

        return dimensionIds;
    }

    private List<Long> getMetricIds(QueryStructReq queryStructCmd) {
        List<Long> metricIds = new ArrayList<>();
        List<MetricResp> metrics = catalog.getMetrics(queryStructCmd.getModelId());
        Map<String, List<MetricResp>> pair = metrics.stream().collect(Collectors.groupingBy(SchemaItem::getBizName));
        for (Aggregator agg : queryStructCmd.getAggregators()) {
            if (pair.containsKey(agg.getColumn())) {
                metricIds.add(pair.get(agg.getColumn()).get(0).getId());
            }
        }
        List<String> filtersCols = sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter());
        for (String col : filtersCols) {
            if (pair.containsKey(col)) {
                metricIds.add(pair.get(col).get(0).getId());
            }
        }
        return metricIds;
    }

    public String getDateWhereClause(QueryStructReq queryStructCmd) {
        DateConf dateInfo = queryStructCmd.getDateInfo();
        if (Objects.isNull(dateInfo) || Objects.isNull(dateInfo.getDateMode())) {
            return "";
        }
        List<Long> dimensionIds = getDimensionIds(queryStructCmd);
        List<Long> metricIds = getMetricIds(queryStructCmd);

        ItemDateResp dateDate = catalog.getItemDate(
                new ItemDateFilter(dimensionIds, TypeEnums.DIMENSION.getName()),
                new ItemDateFilter(metricIds, TypeEnums.METRIC.getName()));
        if (Objects.isNull(dateDate)
                || Strings.isEmpty(dateDate.getStartDate())
                && Strings.isEmpty(dateDate.getEndDate())) {
            if (dateInfo.getDateMode().equals(DateMode.LIST)) {
                return dateUtils.listDateStr(dateDate, dateInfo);
            }
            if (dateInfo.getDateMode().equals(DateMode.BETWEEN)) {
                return dateUtils.betweenDateStr(dateDate, dateInfo);
            }
            if (dateUtils.hasAvailableDataMode(dateInfo)) {
                return dateUtils.hasDataModeStr(dateDate, dateInfo);
            }

            return dateUtils.defaultRecentDateInfo(queryStructCmd.getDateInfo());
        }
        log.info("dateDate:{}", dateDate);
        return dateUtils.getDateWhereStr(dateInfo, dateDate);
    }


    public String generateWhere(QueryStructReq queryStructCmd) {
        String whereClauseFromFilter = sqlFilterUtils.getWhereClause(queryStructCmd.getOriginalFilter());
        String whereFromDate = getDateWhereClause(queryStructCmd);
        if (Strings.isNotEmpty(whereFromDate) && Strings.isNotEmpty(whereClauseFromFilter)) {
            return String.format("%s AND (%s)", whereFromDate, whereClauseFromFilter);
        } else if (Strings.isEmpty(whereFromDate) && Strings.isNotEmpty(whereClauseFromFilter)) {
            return whereClauseFromFilter;
        } else if (Strings.isNotEmpty(whereFromDate) && Strings.isEmpty(whereClauseFromFilter)) {
            return whereFromDate;
        } else if (Strings.isEmpty(whereFromDate) && Strings.isEmpty(whereClauseFromFilter)) {
            log.info("the current date information is empty, enter the date initialization logic");
            return dateUtils.defaultRecentDateInfo(queryStructCmd.getDateInfo());
        }
        return whereClauseFromFilter;
    }

    public Set<String> getResNameEn(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = new HashSet<>();
        queryStructCmd.getAggregators().stream().forEach(agg -> resNameEnSet.add(agg.getColumn()));
        resNameEnSet.addAll(queryStructCmd.getGroups());
        queryStructCmd.getOrders().stream().forEach(order -> resNameEnSet.add(order.getColumn()));
        sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter()).stream().forEach(col -> resNameEnSet.add(col));
        return resNameEnSet;
    }
    public Set<String> getResNameEn(QueryDslReq queryDslReq) {
        Set<String> resNameEnSet = SqlParserSelectHelper.getAllFields(queryDslReq.getSql())
                        .stream().collect(Collectors.toSet());
        return resNameEnSet;
    }
    public Set<String> getResNameEnExceptInternalCol(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = getResNameEn(queryStructCmd);
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }
    public Set<String> getResNameEnExceptInternalCol(QueryDslReq queryDslReq) {
        Set<String> resNameEnSet = getResNameEn(queryDslReq);
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

    public Set<String> getFilterResNameEn(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = new HashSet<>();
        sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter()).stream().forEach(col -> resNameEnSet.add(col));
        return resNameEnSet;
    }

    public Set<String> getFilterResNameEnExceptInternalCol(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = getFilterResNameEn(queryStructCmd);
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

    public Set<String> getFilterResNameEnExceptInternalCol(QueryDslReq queryDslReq) {
        String sql = queryDslReq.getSql();
        Set<String> resNameEnSet = SqlParserSelectHelper.getWhereFields(sql).stream().collect(Collectors.toSet());
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

    public String generateInternalMetricName(Long modelId, List<String> groups) {
        String internalMetricNamePrefix = "";
        if (CollectionUtils.isEmpty(groups)) {
            log.warn("group is empty!");
        } else {
            String group = groups.get(0).equalsIgnoreCase("sys_imp_date")
                    ? groups.get(1) : groups.get(0);
            DimensionResp dimension = catalog.getDimension(group, modelId);
            String datasourceBizName = dimension.getDatasourceBizName();
            if (Strings.isNotEmpty(datasourceBizName)) {
                internalMetricNamePrefix = datasourceBizName + UNDERLINE;
            }

        }
        String internalMetricName = internalMetricNamePrefix + internalMetricNameSuffix;
        return internalMetricName;
    }

}

