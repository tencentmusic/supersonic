package com.tencent.supersonic.chat.utils;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.config.DefaultMetric;
import com.tencent.supersonic.chat.config.Dim4Dict;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static com.tencent.supersonic.common.pojo.Constants.SPACE;
import static com.tencent.supersonic.common.pojo.Constants.AND_UPPER;
import static com.tencent.supersonic.common.pojo.Constants.COMMA;
import static com.tencent.supersonic.common.pojo.Constants.APOSTROPHE;
import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE_DOUBLE;

@Slf4j
@Component
public class DictQueryHelper {

    private static final Long MAX_FREQUENCY = 99999999L;
    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();
    @Value("${dimension.multi.value.split:#}")
    private String dimMultiValueSplit;
    @Value("${dimension.value.show:50}")
    private Integer printDataShow;
    @Value("${dimension.max.limit:3000000}")
    private Long dimMaxLimit;
    @Value("${dimension.white.weight:60000000}")
    private Long dimensionWhiteWeight;

    public List<String> fetchDimValueSingle(Long modelId, DefaultMetric defaultMetricDesc, Dim4Dict dim4Dict,
                                            User user) {
        List<String> data = new ArrayList<>();
        QueryStructReq queryStructCmd = generateQueryStructCmd(modelId, defaultMetricDesc, dim4Dict);
        try {
            QueryResultWithSchemaResp queryResultWithColumns = semanticLayer.queryByStruct(queryStructCmd, user);
            log.info("fetchDimValueSingle sql:{}", queryResultWithColumns.getSql());
            String nature = String.format("_%d_%d", modelId, dim4Dict.getDimId());
            String dimNameRewrite = rewriteDimName(queryResultWithColumns.getColumns(), dim4Dict.getBizName());
            data = generateFileData(queryResultWithColumns.getResultList(), nature, dimNameRewrite,
                    defaultMetricDesc.getBizName(), dim4Dict);
            if (!CollectionUtils.isEmpty(data)) {
                int size = (data.size() > printDataShow) ? printDataShow : data.size();
                log.info("data:{}", data.subList(0, size - 1));
            } else {
                log.warn("data is empty. nature:{}", nature);
                if (Objects.nonNull(queryResultWithColumns)) {
                    log.warn("sql:{}", queryResultWithColumns.getSql());
                }
            }

        } catch (Exception e) {
            log.warn("fetchDimValueSingle,e:", e);
        }
        return data;
    }

    private String rewriteDimName(List<QueryColumn> columns, String bizName) {
        // metric parser join dimension style
        String dimNameRewrite = bizName;

        if (!CollectionUtils.isEmpty(columns)) {
            for (QueryColumn column : columns) {
                if (Strings.isNotEmpty(column.getNameEn())) {
                    String nameEn = column.getNameEn();
                    if (nameEn.endsWith(UNDERLINE_DOUBLE + bizName)) {
                        dimNameRewrite = nameEn;
                    }
                }
            }
        }
        return dimNameRewrite;
    }

    private List<String> generateFileData(List<Map<String, Object>> resultList, String nature, String dimName,
                                          String metricName, Dim4Dict dim4Dict) {
        List<String> data = new ArrayList<>();
        if (CollectionUtils.isEmpty(resultList)) {
            return data;
        }
        Map<String, Long> valueAndFrequencyPair = new HashMap<>(2000);
        for (Map<String, Object> line : resultList) {

            if (CollectionUtils.isEmpty(line) || !line.containsKey(dimName)) {
                continue;
            }

            String dimValue = line.get(dimName).toString();
            Object metricObject = line.get(metricName);
            if (Strings.isNotEmpty(dimValue) && Objects.nonNull(metricObject)) {
                Long metric = Math.round(Double.parseDouble(metricObject.toString()));
                mergeMultivaluedValue(valueAndFrequencyPair, dimValue, metric);
            }

        }
        constructDataLines(valueAndFrequencyPair, nature, data, dim4Dict);
        return data;
    }

    private void constructDataLines(Map<String, Long> valueAndFrequencyPair, String nature,
                                    List<String> data, Dim4Dict dim4Dict) {
        valueAndFrequencyPair.forEach((dimValue, metric) -> {
            if (metric > MAX_FREQUENCY) {
                metric = MAX_FREQUENCY;
            }
            if (Strings.isNotEmpty(dimValue) && dimValue.contains(SPACE)) {
                dimValue = dimValue.replace(SPACE, "#");
            }
            data.add(String.format("%s %s %s", dimValue, nature, metric));
        });

        if (Objects.nonNull(dim4Dict) && !CollectionUtils.isEmpty(dim4Dict.getWhiteList())) {
            dim4Dict.getWhiteList().stream()
                    .forEach(white -> data.add(String.format("%s %s %s", white, nature, dimensionWhiteWeight)));
        }
    }

    private void mergeMultivaluedValue(Map<String, Long> valueAndFrequencyPair, String dimValue, Long metric) {
        if (Strings.isEmpty(dimValue)) {
            return;
        }
        Map<String, Long> tmp = new HashMap<>();
        if (dimValue.contains(dimMultiValueSplit)) {
            Arrays.stream(dimValue.split(dimMultiValueSplit))
                    .forEach(dimValueSingle -> tmp.put(dimValueSingle, metric));
        } else {
            tmp.put(dimValue, metric);
        }

        for (String value : tmp.keySet()) {
            long metricOld = valueAndFrequencyPair.containsKey(value) ? valueAndFrequencyPair.get(value) : 0L;
            valueAndFrequencyPair.put(value, metric + metricOld);
        }
    }

    private QueryStructReq generateQueryStructCmd(Long modelId, DefaultMetric defaultMetricDesc, Dim4Dict dim4Dict) {
        QueryStructReq queryStructCmd = new QueryStructReq();

        queryStructCmd.setModelId(modelId);
        queryStructCmd.setGroups(Arrays.asList(dim4Dict.getBizName()));

        List<Filter> filters = generateFilters(dim4Dict, queryStructCmd);
        queryStructCmd.setDimensionFilters(filters);

        List<Aggregator> aggregators = new ArrayList<>();
        aggregators.add(new Aggregator(defaultMetricDesc.getBizName(), AggOperatorEnum.SUM));
        queryStructCmd.setAggregators(aggregators);

        List<Order> orders = new ArrayList<>();
        orders.add(new Order(defaultMetricDesc.getBizName(), Constants.DESC_UPPER));
        queryStructCmd.setOrders(orders);

        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.RECENT);
        dateInfo.setUnit(defaultMetricDesc.getUnit());
        queryStructCmd.setDateInfo(dateInfo);

        queryStructCmd.setLimit(dimMaxLimit);
        return queryStructCmd;

    }

    private List<Filter> generateFilters(Dim4Dict dim4Dict, QueryStructReq queryStructCmd) {
        String whereStr = generateFilter(dim4Dict);
        if (Strings.isEmpty(whereStr)) {
            return new ArrayList<>();
        }
        Filter filter = new Filter("", FilterOperatorEnum.SQL_PART, whereStr);
        List<Filter> filters = Objects.isNull(queryStructCmd.getOriginalFilter()) ? new ArrayList<>()
                : queryStructCmd.getOriginalFilter();
        filters.add(filter);
        return filters;
    }

    private String generateFilter(Dim4Dict dim4Dict) {
        if (Objects.isNull(dim4Dict)) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(SPACE + AND_UPPER + SPACE);

        String dimName = dim4Dict.getBizName();
        if (!CollectionUtils.isEmpty(dim4Dict.getBlackList())) {
            StringJoiner joinerBlack = new StringJoiner(COMMA);
            dim4Dict.getBlackList().stream().forEach(black -> joinerBlack.add(APOSTROPHE + black + APOSTROPHE));
            joiner.add(String.format("(%s not in (%s))", dimName, joinerBlack.toString()));
        }

        if (!CollectionUtils.isEmpty(dim4Dict.getRuleList())) {
            dim4Dict.getRuleList().stream().forEach(rule -> joiner.add(rule));
        }

        return joiner.toString();
    }
}