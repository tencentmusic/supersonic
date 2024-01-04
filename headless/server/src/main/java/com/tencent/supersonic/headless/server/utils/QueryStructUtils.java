package com.tencent.supersonic.headless.server.utils;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.common.util.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.request.ModelSchemaFilterReq;
import com.tencent.supersonic.headless.api.request.QueryS2SQLReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.response.ModelSchemaResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.Catalog;
import com.tencent.supersonic.headless.server.service.SchemaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private SchemaService schemaService;

    private String variablePrefix = "'${";

    public QueryStructUtils(
            DateModeUtils dateModeUtils,
            SqlFilterUtils sqlFilterUtils, Catalog catalog) {

        this.dateModeUtils = dateModeUtils;
        this.sqlFilterUtils = sqlFilterUtils;
        this.catalog = catalog;
    }

    private List<Long> getDimensionIds(QueryStructReq queryStructCmd) {
        List<Long> dimensionIds = new ArrayList<>();
        MetaFilter metaFilter = new MetaFilter(queryStructCmd.getModelIds());
        List<DimensionResp> dimensions = catalog.getDimensions(metaFilter);
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
        MetaFilter metaFilter = new MetaFilter(queryStructCmd.getModelIds());
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

    public Set<String> getResName(QueryS2SQLReq queryS2SQLReq) {
        Set<String> resNameSet = SqlParserSelectHelper.getAllFields(queryS2SQLReq.getSql())
                .stream().collect(Collectors.toSet());
        return resNameSet;
    }

    public Set<String> getResNameEnExceptInternalCol(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = getResNameEn(queryStructCmd);
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

    public Set<String> getResNameEnExceptInternalCol(QueryS2SQLReq queryS2SQLReq, User user) {
        Set<String> resNameSet = getResName(queryS2SQLReq);
        Set<String> resNameEnSet = new HashSet<>();
        ModelSchemaFilterReq filter = new ModelSchemaFilterReq();
        List<Long> modelIds = Lists.newArrayList(queryS2SQLReq.getModelIds());
        filter.setModelIds(modelIds);
        List<ModelSchemaResp> modelSchemaRespList = schemaService.fetchModelSchema(filter, user);
        if (!CollectionUtils.isEmpty(modelSchemaRespList)) {
            List<MetricSchemaResp> metrics = modelSchemaRespList.get(0).getMetrics();
            List<DimSchemaResp> dimensions = modelSchemaRespList.get(0).getDimensions();
            metrics.stream().forEach(o -> {
                if (resNameSet.contains(o.getName())) {
                    resNameEnSet.add(o.getBizName());
                }
            });
            dimensions.stream().forEach(o -> {
                if (resNameSet.contains(o.getName())) {
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

    public Set<String> getFilterResNameEnExceptInternalCol(QueryS2SQLReq queryS2SQLReq) {
        String sql = queryS2SQLReq.getSql();
        Set<String> resNameEnSet = SqlParserSelectHelper.getWhereFields(sql).stream().collect(Collectors.toSet());
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

    public ItemDateResp getItemDateResp(QueryStructReq queryStructCmd) {
        List<Long> dimensionIds = getDimensionIds(queryStructCmd);
        List<Long> metricIds = getMetricIds(queryStructCmd);
        ItemDateResp dateDate = catalog.getItemDate(
                new ItemDateFilter(dimensionIds, TypeEnums.DIMENSION.getName()),
                new ItemDateFilter(metricIds, TypeEnums.METRIC.getName()));
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
        List<FieldExpression> fieldExpressions = SqlParserSelectHelper.getFilterExpression(sql);
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

