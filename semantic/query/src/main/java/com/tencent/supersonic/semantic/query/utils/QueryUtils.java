package com.tencent.supersonic.semantic.query.utils;

import static com.tencent.supersonic.common.pojo.Constants.JOIN_UNDERLINE;
import static com.tencent.supersonic.common.pojo.Constants.UNIONALL;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.util.cache.CacheUtils;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Slf4j
@Component
public class QueryUtils {

    private final Set<Pattern> patterns = new HashSet<>();
    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;

    private final CacheUtils cacheUtils;
    private final StatUtils statUtils;

    private final Catalog catalog;

    public QueryUtils(
            CacheUtils cacheUtils, StatUtils statUtils, Catalog catalog) {

        this.cacheUtils = cacheUtils;
        this.statUtils = statUtils;
        this.catalog = catalog;
    }

    @PostConstruct
    public void fillPattern() {
        Set<String> aggFunctions = new HashSet<>(Arrays.asList("MAX", "MIN", "SUM", "AVG"));
        String patternStr = "\\s*(%s\\((.*)\\)) AS";
        for (String agg : aggFunctions) {
            patterns.add(Pattern.compile(String.format(patternStr, agg)));
        }
    }

    public void fillItemNameInfo(QueryResultWithSchemaResp queryResultWithColumns, Long modelId) {
        List<MetricResp> metricDescList = catalog.getMetrics(modelId);
        List<DimensionResp> dimensionDescList = catalog.getDimensions(modelId);
        Map<String, MetricResp> metricRespMap =
                metricDescList.stream().collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
        Map<String, String> namePair = new HashMap<>();
        Map<String, String> nameTypePair = new HashMap<>();
        addSysTimeDimension(namePair, nameTypePair);
        metricDescList.forEach(metricDesc -> {
            namePair.put(metricDesc.getBizName(), metricDesc.getName());
            nameTypePair.put(metricDesc.getBizName(), "NUMBER");
        });
        dimensionDescList.forEach(dimensionDesc -> {
            namePair.put(dimensionDesc.getBizName(), dimensionDesc.getName());
            nameTypePair.put(dimensionDesc.getBizName(), dimensionDesc.getSemanticType());
        });
        List<QueryColumn> columns = queryResultWithColumns.getColumns();
        columns.forEach(column -> {
            String nameEn = column.getNameEn().toLowerCase();
            if (nameEn.contains(JOIN_UNDERLINE)) {
                nameEn = nameEn.split(JOIN_UNDERLINE)[1];
            }
            if (namePair.containsKey(nameEn)) {
                column.setName(namePair.get(nameEn));
            }
            if (nameTypePair.containsKey(nameEn)) {
                column.setShowType(nameTypePair.get(nameEn));
            }
            if (!nameTypePair.containsKey(nameEn) && isNumberType(column.getType())) {
                column.setShowType("NUMBER");
            }
            if (metricRespMap.containsKey(nameEn)) {
                column.setDataFormatType(metricRespMap.get(nameEn).getDataFormatType());
                column.setDataFormat(metricRespMap.get(nameEn).getDataFormat());
            }
            if (StringUtils.isEmpty(column.getShowType())) {
                column.setShowType("NUMBER");
            }
        });
    }

    public void fillItemNameInfo(QueryResultWithSchemaResp queryResultWithColumns,
            QueryMultiStructReq queryMultiStructCmd) {
        List<Aggregator> aggregators = queryMultiStructCmd.getQueryStructReqs().stream()
                .flatMap(queryStructCmd -> queryStructCmd.getAggregators().stream())
                .collect(Collectors.toList());
        log.info("multi agg merge:{}", aggregators);
        Map<String, String> metricNameFromAgg = getMetricNameFromAgg(aggregators);
        log.info("metricNameFromAgg:{}", metricNameFromAgg);
        Map<String, String> namePair = new HashMap<>();
        Map<String, String> nameTypePair = new HashMap<>();
        addSysTimeDimension(namePair, nameTypePair);
        namePair.putAll(metricNameFromAgg);
        List<QueryColumn> columns = queryResultWithColumns.getColumns();
        columns.forEach(column -> {
            String nameEn = column.getNameEn().toLowerCase();
            if (nameEn.contains(JOIN_UNDERLINE)) {
                nameEn = nameEn.split(JOIN_UNDERLINE)[1];
            }
            if (namePair.containsKey(nameEn)) {
                column.setName(namePair.get(nameEn));
            } else {
                if (nameEn.startsWith("name")) {
                    column.setName("名称");
                } else if (nameEn.startsWith("value")) {
                    column.setName("指标值");
                }
            }
            if (nameTypePair.containsKey(nameEn)) {
                column.setShowType(nameTypePair.get(nameEn));
            } else {
                if (nameEn.startsWith("name")) {
                    column.setShowType("CATEGORY");
                } else if (nameEn.startsWith("value")) {
                    column.setShowType("NUMBER");
                }
            }
        });
    }

    private boolean isNumberType(String type) {
        if (StringUtils.isBlank(type)) {
            return false;
        }
        if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("bigint")
                || type.equalsIgnoreCase("float") || type.equalsIgnoreCase("double")) {
            return true;
        }
        if (type.toLowerCase().startsWith("uint") || type.toLowerCase().startsWith("int")) {
            return true;
        }
        return false;
    }

    private Map<String, String> getMetricNameFromAgg(List<Aggregator> aggregators) {
        Map<String, String> map = new HashMap<>();
        if (CollectionUtils.isEmpty(aggregators)) {
            return map;
        }
        for (int i = 0; i < aggregators.size(); i++) {
            Aggregator aggregator = aggregators.get(i);
            if (StringUtils.isBlank(aggregator.getNameCh())) {
                continue;
            }
            map.put("value" + (i + 1), aggregator.getNameCh());
        }
        return map;
    }

    private static void addSysTimeDimension(Map<String, String> namePair, Map<String, String> nameTypePair) {
        for (TimeDimensionEnum timeDimensionEnum : TimeDimensionEnum.values()) {
            namePair.put(timeDimensionEnum.getName(), "date");
            nameTypePair.put(timeDimensionEnum.getName(), "DATE");
        }
    }


    public void checkSqlParse(QueryStatement sqlParser) {
        if (com.google.common.base.Strings.isNullOrEmpty(sqlParser.getSql())
                || com.google.common.base.Strings.isNullOrEmpty(sqlParser.getSourceId())) {
            throw new RuntimeException("parse Exception: " + sqlParser.getErrMsg());
        }
    }


    public QueryStatement sqlParserUnion(QueryMultiStructReq queryMultiStructCmd, List<QueryStatement> sqlParsers) {
        QueryStatement sqlParser = new QueryStatement();
        StringBuilder unionSqlBuilder = new StringBuilder();
        for (int i = 0; i < sqlParsers.size(); i++) {
            String selectStr = SqlGenerateUtils.getUnionSelect(queryMultiStructCmd.getQueryStructReqs().get(i));
            unionSqlBuilder.append(String.format("select %s from ( %s ) sub_sql_%s",
                    selectStr,
                    sqlParsers.get(i).getSql(), i));
            unionSqlBuilder.append(UNIONALL);
        }
        String unionSql = unionSqlBuilder.substring(0, unionSqlBuilder.length() - Constants.UNIONALL.length());
        sqlParser.setSql(unionSql);
        sqlParser.setSourceId(sqlParsers.get(0).getSourceId());
        log.info("union sql parser:{}", sqlParser);
        return sqlParser;
    }

    public void cacheResultLogic(String key, QueryResultWithSchemaResp queryResultWithColumns) {
        if (cacheEnable && Objects.nonNull(queryResultWithColumns) && !CollectionUtils.isEmpty(
                queryResultWithColumns.getResultList())) {
            QueryResultWithSchemaResp finalQueryResultWithColumns = queryResultWithColumns;
            CompletableFuture.supplyAsync(() -> cacheUtils.put(key, finalQueryResultWithColumns))
                    .exceptionally(exception -> {
                        log.warn("exception:", exception);
                        return null;
                    });
            statUtils.updateResultCacheKey(key);
            log.info("add record to cache, key:{}", key);
        }

    }
}
