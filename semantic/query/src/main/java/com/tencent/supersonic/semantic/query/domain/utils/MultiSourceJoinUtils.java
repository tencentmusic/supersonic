package com.tencent.supersonic.semantic.query.domain.utils;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.semantic.api.core.pojo.Identify;
import com.tencent.supersonic.semantic.api.core.pojo.Measure;
import com.tencent.supersonic.semantic.api.core.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.semantic.core.domain.DatasourceService;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.MetricService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class MultiSourceJoinUtils {


    private DatasourceService datasourceService;

    private DimensionService dimensionService;

    private MetricService metricService;


    public MultiSourceJoinUtils(DatasourceService datasourceService, DimensionService dimensionService,
            MetricService metricService) {
        this.datasourceService = datasourceService;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
    }


    public void buildJoinPrefix(QueryStructReq queryStructCmd) {
        List<String> groups = queryStructCmd.getGroups();
        List<Aggregator> aggregators = queryStructCmd.getAggregators();
        List<Filter> filters = queryStructCmd.getOriginalFilter();
        List<String> fields = Lists.newArrayList();
        fields.addAll(groups);
        fields.addAll(filters.stream().map(Filter::getBizName).collect(Collectors.toList()));

        if (CollectionUtils.isEmpty(groups) || CollectionUtils.isEmpty(aggregators)) {
            return;
        }
        Long domainId = queryStructCmd.getDomainId();
        List<String> aggs = aggregators.stream().map(Aggregator::getColumn).collect(Collectors.toList());
        Map<String, DimensionResp> dimensionMap = dimensionService.getDimensions(domainId).stream()
                .filter(dimensionDesc -> fields.contains(dimensionDesc.getBizName()))
                .collect(Collectors.toMap(DimensionResp::getBizName, dimensionDesc -> dimensionDesc));
        List<MetricResp> metricDescList = metricService.getMetrics(domainId).stream()
                .filter(metricDesc -> aggs.contains(metricDesc.getBizName()))
                .collect(Collectors.toList());
        Map<Long, DatasourceResp> datasourceMap = datasourceService.getDatasourceList(domainId)
                .stream().collect(Collectors.toMap(DatasourceResp::getId, datasource -> datasource));
        //check groups filters and aggs is in same datasource
        if (!isInSameDatasource(new ArrayList<>(dimensionMap.values()), metricDescList)) {
            List<String> groupsWithPrefix = Lists.newArrayList();
            for (String group : groups) {
                DimensionResp dimensionDesc = dimensionMap.get(group);
                if (dimensionDesc == null) {
                    groupsWithPrefix.add(group);
                    continue;
                }
                String joinKeyName = getJoinKey(datasourceMap, dimensionDesc.getDatasourceId());
                if (joinKeyName.equalsIgnoreCase(group)) {
                    groupsWithPrefix.add(group);
                } else {
                    String groupWithPrefix = String.format("%s__%s", joinKeyName, group);
                    groupsWithPrefix.add(groupWithPrefix);
                }
            }
            List<Filter> filtersWithPrefix = Lists.newArrayList();
            for (Filter filter : filters) {
                DimensionResp dimensionDesc = dimensionMap.get(filter.getBizName());
                if (dimensionDesc == null) {
                    filtersWithPrefix.add(filter);
                    continue;
                }
                String joinKeyName = getJoinKey(datasourceMap, dimensionDesc.getDatasourceId());
                if (joinKeyName.equalsIgnoreCase(filter.getBizName())) {
                    filtersWithPrefix.add(filter);
                } else {
                    String filterWithPrefix = String.format("%s__%s", joinKeyName, filter.getBizName());
                    filter.setBizName(filterWithPrefix);
                    filtersWithPrefix.add(filter);
                }
            }
            queryStructCmd.setGroups(groupsWithPrefix);
            queryStructCmd.setDimensionFilters(filtersWithPrefix);
        }
    }


    private String getJoinKey(Map<Long, DatasourceResp> datasourceMap, Long datasourceId) {
        DatasourceResp datasourceDesc = datasourceMap.get(datasourceId);
        List<Identify> identifies = datasourceDesc.getDatasourceDetail().getIdentifiers();

        Optional<Identify> identifyOptional = identifies.stream()
                .filter(identify -> identify.getType().equalsIgnoreCase("primary")).findFirst();
        if (identifyOptional.isPresent()) {
            return identifyOptional.get().getBizName();
        }
        return "";
    }


    private boolean isInSameDatasource(List<DimensionResp> dimensionDescs, List<MetricResp> metricDescs) {
        Set<Long> datasourceIdSet = Sets.newHashSet();
        datasourceIdSet.addAll(dimensionDescs.stream().map(DimensionResp::getDatasourceId).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        datasourceIdSet.addAll(
                metricDescs.stream().flatMap(metricDesc -> metricDesc.getTypeParams().getMeasures().stream())
                        .map(Measure::getDatasourceId).filter(Objects::nonNull).collect(Collectors.toList()));
        log.info("[multi source join] datasource id:{}", datasourceIdSet);
        return datasourceIdSet.size() <= 1;

    }


}
