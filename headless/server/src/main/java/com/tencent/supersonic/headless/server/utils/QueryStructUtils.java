package com.tencent.supersonic.headless.server.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.server.service.SchemaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.DAY_FORMAT;

@Slf4j
@Component
public class QueryStructUtils {

    public static Set<String> internalCols =
            new HashSet<>(Arrays.asList("dayno", "sys_imp_date", "sys_imp_week", "sys_imp_month"));

    private final DateModeUtils dateModeUtils;
    private final SqlFilterUtils sqlFilterUtils;
    private final SchemaService schemaService;

    public QueryStructUtils(DateModeUtils dateModeUtils, SqlFilterUtils sqlFilterUtils,
            SchemaService schemaService) {

        this.dateModeUtils = dateModeUtils;
        this.sqlFilterUtils = sqlFilterUtils;
        this.schemaService = schemaService;
    }

    private List<Long> getDimensionIds(QueryStructReq queryStructReq) {
        List<Long> dimensionIds = new ArrayList<>();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setDataSetId(queryStructReq.getDataSetId());
        List<DimensionResp> dimensions = schemaService.getDimensions(metaFilter);
        Map<String, List<DimensionResp>> pair =
                dimensions.stream().collect(Collectors.groupingBy(DimensionResp::getBizName));
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
        List<MetricResp> metrics = schemaService.getMetrics(metaFilter);
        Map<String, List<MetricResp>> pair =
                metrics.stream().collect(Collectors.groupingBy(SchemaItem::getBizName));
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

    public Set<String> getBizNameFromStruct(QueryStructReq queryStructReq) {
        Set<String> resNameEnSet = new HashSet<>();
        queryStructReq.getAggregators().stream().forEach(agg -> resNameEnSet.add(agg.getColumn()));
        resNameEnSet.addAll(queryStructReq.getGroups());
        queryStructReq.getOrders().stream().forEach(order -> resNameEnSet.add(order.getColumn()));
        sqlFilterUtils.getFiltersCol(queryStructReq.getOriginalFilter()).stream()
                .forEach(col -> resNameEnSet.add(col));
        return resNameEnSet;
    }

    public Set<String> getResName(QuerySqlReq querySqlReq) {
        return new HashSet<>(SqlSelectHelper.getAllSelectFields(querySqlReq.getSql()));
    }

    public Set<Long> getModelIdsFromStruct(QueryStructReq queryStructReq,
            SemanticSchemaResp semanticSchemaResp) {
        Set<Long> modelIds = Sets.newHashSet();
        Set<String> bizNameFromStruct = getBizNameFromStruct(queryStructReq);
        modelIds.addAll(semanticSchemaResp.getMetrics().stream()
                .filter(metric -> bizNameFromStruct.contains(metric.getBizName()))
                .map(MetricResp::getModelId).collect(Collectors.toSet()));
        modelIds.addAll(semanticSchemaResp.getDimensions().stream()
                .filter(dimension -> bizNameFromStruct.contains(dimension.getBizName()))
                .map(DimensionResp::getModelId).collect(Collectors.toList()));
        return modelIds;
    }

    private List<MetricResp> getMetricsFromSql(QuerySqlReq querySqlReq,
            SemanticSchemaResp semanticSchemaResp) {
        Set<String> resNameSet = getResName(querySqlReq);
        if (semanticSchemaResp != null) {
            return semanticSchemaResp.getMetrics().stream().filter(
                    m -> resNameSet.contains(m.getName()) || resNameSet.contains(m.getBizName()))
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    private Set<Long> getModelIdsByIdentifiesFromSql(QuerySqlReq querySqlReq,
            SemanticSchemaResp semanticSchemaResp) {
        Set<String> resNameSet = getResName(querySqlReq);
        Set<Long> modelIds = new HashSet<>();

        if (semanticSchemaResp == null) {
            return modelIds;
        }
        if (CollectionUtils.isEmpty(semanticSchemaResp.getModelResps())) {
            return modelIds;
        }
        for (ModelResp modelResp : semanticSchemaResp.getModelResps()) {
            if (modelHasMatchingIdentifier(modelResp, resNameSet)) {
                modelIds.add(modelResp.getId());
            }
        }
        return modelIds;
    }

    private boolean modelHasMatchingIdentifier(ModelResp modelResp, Set<String> resNameSet) {
        if (modelResp.getModelDetail() == null) {
            return false;
        }
        List<Identify> identifiers = modelResp.getModelDetail().getIdentifiers();
        if (CollectionUtils.isEmpty(identifiers)) {
            return false;
        }
        return identifiers.stream().anyMatch(identifier -> resNameSet.contains(identifier.getName())
                || resNameSet.contains(identifier.getBizName()));
    }

    private List<DimensionResp> getDimensionsFromSql(QuerySqlReq querySqlReq,
            SemanticSchemaResp semanticSchemaResp) {
        Set<String> resNameSet = getResName(querySqlReq);
        if (semanticSchemaResp != null) {
            return semanticSchemaResp.getDimensions().stream().filter(
                    m -> resNameSet.contains(m.getName()) || resNameSet.contains(m.getBizName()))
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public Set<Long> getModelIdFromSql(QuerySqlReq querySqlReq,
            SemanticSchemaResp semanticSchemaResp) {
        Set<Long> modelIds = Sets.newHashSet();
        List<DimensionResp> dimensions = getDimensionsFromSql(querySqlReq, semanticSchemaResp);
        List<MetricResp> metrics = getMetricsFromSql(querySqlReq, semanticSchemaResp);
        modelIds.addAll(
                dimensions.stream().map(DimensionResp::getModelId).collect(Collectors.toList()));
        modelIds.addAll(metrics.stream().map(MetricResp::getModelId).collect(Collectors.toList()));
        modelIds.addAll(getModelIdsByIdentifiesFromSql(querySqlReq, semanticSchemaResp));
        return modelIds;
    }

    public Set<String> getBizNameFromSql(QuerySqlReq querySqlReq,
            SemanticSchemaResp semanticSchemaResp) {
        Set<String> bizNames = Sets.newHashSet();
        List<DimensionResp> dimensions = getDimensionsFromSql(querySqlReq, semanticSchemaResp);
        List<MetricResp> metrics = getMetricsFromSql(querySqlReq, semanticSchemaResp);
        bizNames.addAll(
                dimensions.stream().map(DimensionResp::getBizName).collect(Collectors.toList()));
        bizNames.addAll(metrics.stream().map(MetricResp::getBizName).collect(Collectors.toList()));
        return bizNames;
    }

    public ItemDateResp getItemDateResp(QueryStructReq queryStructCmd) {
        List<Long> dimensionIds = getDimensionIds(queryStructCmd);
        List<Long> metricIds = getMetricIds(queryStructCmd);
        ItemDateResp dateDate = schemaService.getItemDate(
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
                    return Triple.of(dateInfo,
                            dateMin.format(DateTimeFormatter.ofPattern(DAY_FORMAT)),
                            dateMax.format(DateTimeFormatter.ofPattern(DAY_FORMAT)));
                }
                switch (dateConf.getPeriod()) {
                    case DAY:
                        ImmutablePair<String, String> dayInfo =
                                dateModeUtils.recentDay(dateDate, dateConf);
                        return Triple.of(dateInfo, dayInfo.left, dayInfo.right);
                    case WEEK:
                        ImmutablePair<String, String> weekInfo =
                                dateModeUtils.recentWeek(dateDate, dateConf);
                        return Triple.of(dateInfo, weekInfo.left, weekInfo.right);
                    case MONTH:
                        List<ImmutablePair<String, String>> rets =
                                dateModeUtils.recentMonth(dateDate, dateConf);
                        Optional<String> minBegins =
                                rets.stream().map(i -> i.left).sorted().findFirst();
                        Optional<String> maxBegins = rets.stream().map(i -> i.right)
                                .sorted(Comparator.reverseOrder()).findFirst();
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

}
