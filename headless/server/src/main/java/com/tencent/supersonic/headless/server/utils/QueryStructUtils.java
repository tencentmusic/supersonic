package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.common.util.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.Catalog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.DAY;
import static com.tencent.supersonic.common.pojo.Constants.DAY_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.MONTH;
import static com.tencent.supersonic.common.pojo.Constants.WEEK;


@Slf4j
@Component
public class QueryStructUtils {

    public static Set<String> internalTimeCols = new HashSet<>(
            Arrays.asList("dayno", "sys_imp_date", "sys_imp_week", "sys_imp_month"));
    public static Set<String> internalCols;

    static {
        internalCols = new HashSet<>(Arrays.asList("plat_sys_var"));
        internalCols.addAll(internalTimeCols);
    }

    private final DateModeUtils dateModeUtils;
    private final SqlFilterUtils sqlFilterUtils;
    private final Catalog catalog;
    private String variablePrefix = "'${";

    public QueryStructUtils(
            DateModeUtils dateModeUtils,
            SqlFilterUtils sqlFilterUtils, @Lazy Catalog catalog) {

        this.dateModeUtils = dateModeUtils;
        this.sqlFilterUtils = sqlFilterUtils;
        this.catalog = catalog;
    }

    private List<Long> getDimensionIds(QueryStructReq queryStructReq) {
        List<Long> dimensionIds = new ArrayList<>();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setDataSetId(queryStructReq.getDataSetId());
        List<DimensionResp> dimensions = catalog.getDimensions(metaFilter);
        Map<String, List<DimensionResp>> pair = dimensions.stream()
                .collect(Collectors.groupingBy(DimensionResp::getBizName));
        for (String group : queryStructReq.getGroups()) {
            if (pair.containsKey(group)) {
                dimensionIds.add(pair.get(group).get(0).getId());
            }
        }

        List<String> filtersCols = sqlFilterUtils.getFiltersCol(queryStructReq.getOriginalFilter());
        for (String col : filtersCols) {
            if (pair.containsKey(col)) {
                dimensionIds.add(pair.get(col).get(0).getId());
            }
        }
        return dimensionIds;
    }

    private List<Long> getMetricIds(QueryStructReq queryStructCmd) {
        List<Long> metricIds = new ArrayList<>();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setDataSetId(queryStructCmd.getDataSetId());
        List<MetricResp> metrics = catalog.getMetrics(metaFilter);
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

    public Set<String> getResNameEn(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = new HashSet<>();
        queryStructCmd.getAggregators().stream().forEach(agg -> resNameEnSet.add(agg.getColumn()));
        resNameEnSet.addAll(queryStructCmd.getGroups());
        queryStructCmd.getOrders().stream().forEach(order -> resNameEnSet.add(order.getColumn()));
        sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter()).stream().forEach(col -> resNameEnSet.add(col));
        return resNameEnSet;
    }

    public Set<String> getResName(QuerySqlReq querySqlReq) {
        Set<String> resNameSet = SqlSelectHelper.getAllFields(querySqlReq.getSql())
                .stream().collect(Collectors.toSet());
        return resNameSet;
    }

    public Set<String> getResNameEnExceptInternalCol(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = getResNameEn(queryStructCmd);
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

    public Set<String> getResNameEnExceptInternalCol(QuerySqlReq querySqlReq,
                                                     SemanticSchemaResp semanticSchemaResp) {
        Set<String> resNameSet = getResName(querySqlReq);
        Set<String> resNameEnSet = new HashSet<>();
        if (semanticSchemaResp != null) {
            List<MetricSchemaResp> metrics = semanticSchemaResp.getMetrics();
            List<DimSchemaResp> dimensions = semanticSchemaResp.getDimensions();
            metrics.stream().forEach(o -> {
                if (resNameSet.contains(o.getName()) || resNameSet.contains(o.getBizName())) {
                    resNameEnSet.add(o.getBizName());
                }
            });
            dimensions.stream().forEach(o -> {
                if (resNameSet.contains(o.getName()) || resNameSet.contains(o.getBizName())) {
                    resNameEnSet.add(o.getBizName());
                }
            });
        }
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

    public Set<String> getFilterResNameEnExceptInternalCol(QuerySqlReq querySqlReq) {
        String sql = querySqlReq.getSql();
        Set<String> resNameEnSet = SqlSelectHelper.getWhereFields(sql).stream().collect(Collectors.toSet());
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

    public ItemDateResp getItemDateResp(QueryStructReq queryStructCmd) {
        List<Long> dimensionIds = getDimensionIds(queryStructCmd);
        List<Long> metricIds = getMetricIds(queryStructCmd);
        ItemDateResp dateDate = catalog.getItemDate(
                new ItemDateFilter(dimensionIds, TypeEnums.DIMENSION.name()),
                new ItemDateFilter(metricIds, TypeEnums.METRIC.name()));
        return dateDate;
    }

    public Triple<String, String, String> getBeginEndTime(QueryStructReq queryStructCmd) {
        if (Objects.isNull(queryStructCmd.getDateInfo())) {
            return Triple.of("", "", "");
        }
        DateConf dateConf = queryStructCmd.getDateInfo();
        String dateInfo = dateModeUtils.getSysDateCol(dateConf);
        if (dateInfo.isEmpty()) {
            return Triple.of("", "", "");
        }
        switch (dateConf.getDateMode()) {
            case AVAILABLE:
            case BETWEEN:
                return Triple.of(dateInfo, dateConf.getStartDate(), dateConf.getEndDate());
            case LIST:
                return Triple.of(dateInfo, Collections.min(dateConf.getDateList()),
                        Collections.max(dateConf.getDateList()));
            case RECENT:
                ItemDateResp dateDate = getItemDateResp(queryStructCmd);
                LocalDate dateMax = LocalDate.now().minusDays(1);
                LocalDate dateMin = dateMax.minusDays(dateConf.getUnit() - 1);
                if (Objects.isNull(dateDate)) {
                    return Triple.of(dateInfo, dateMin.format(DateTimeFormatter.ofPattern(DAY_FORMAT)),
                            dateMax.format(DateTimeFormatter.ofPattern(DAY_FORMAT)));
                }
                switch (dateConf.getPeriod()) {
                    case DAY:
                        ImmutablePair<String, String> dayInfo = dateModeUtils.recentDay(dateDate, dateConf);
                        return Triple.of(dateInfo, dayInfo.left, dayInfo.right);
                    case WEEK:
                        ImmutablePair<String, String> weekInfo = dateModeUtils.recentWeek(dateDate, dateConf);
                        return Triple.of(dateInfo, weekInfo.left, weekInfo.right);
                    case MONTH:
                        List<ImmutablePair<String, String>> rets = dateModeUtils.recentMonth(dateDate, dateConf);
                        Optional<String> minBegins = rets.stream().map(i -> i.left).sorted().findFirst();
                        Optional<String> maxBegins = rets.stream().map(i -> i.right).sorted(Comparator.reverseOrder())
                                .findFirst();
                        if (minBegins.isPresent() && maxBegins.isPresent()) {
                            return Triple.of(dateInfo, minBegins.get(), maxBegins.get());
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;

        }
        return Triple.of("", "", "");
    }

    public DateConf getDateConfBySql(String sql) {
        List<FieldExpression> fieldExpressions = SqlSelectHelper.getFilterExpression(sql);
        if (!CollectionUtils.isEmpty(fieldExpressions)) {
            Set<String> dateList = new HashSet<>();
            String startDate = "";
            String endDate = "";
            String period = "";
            for (FieldExpression f : fieldExpressions) {
                if (Objects.isNull(f.getFieldName()) || !internalCols.contains(f.getFieldName().toLowerCase())) {
                    continue;
                }
                if (Objects.isNull(f.getFieldValue()) || !dateModeUtils.isDateStr(f.getFieldValue().toString())) {
                    continue;
                }
                period = dateModeUtils.getPeriodByCol(f.getFieldName().toLowerCase());
                if ("".equals(period)) {
                    continue;
                }
                if ("=".equals(f.getOperator())) {
                    dateList.add(f.getFieldValue().toString());
                } else if ("<".equals(f.getOperator()) || "<=".equals(f.getOperator())) {
                    if (startDate.isEmpty() || startDate.compareTo(f.getFieldValue().toString()) > 0) {
                        startDate = f.getFieldValue().toString();
                    }
                } else if (">".equals(f.getOperator()) || ">=".equals(f.getOperator())) {
                    if (endDate.isEmpty() || endDate.compareTo(f.getFieldValue().toString()) < 0) {
                        endDate = f.getFieldValue().toString();
                    }
                }
            }
            if (!"".equals(period)) {
                DateConf dateConf = new DateConf();
                dateConf.setPeriod(period);
                if (!CollectionUtils.isEmpty(dateList)) {
                    dateConf.setDateList(new ArrayList<>(dateList));
                    dateConf.setDateMode(DateMode.LIST);
                    return dateConf;
                }
                if (!"".equals(startDate) && !"".equals(endDate)) {
                    dateConf.setStartDate(startDate);
                    dateConf.setEndDate(endDate);
                    dateConf.setDateMode(DateMode.BETWEEN);
                    return dateConf;
                }
            }
        }
        return null;
    }

    public List<String> getDateCol() {
        return dateModeUtils.getDateCol();
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

}

