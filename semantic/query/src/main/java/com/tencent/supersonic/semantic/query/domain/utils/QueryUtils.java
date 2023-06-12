package com.tencent.supersonic.semantic.query.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.END_SUBQUERY;
import static com.tencent.supersonic.common.constant.Constants.GROUP_UPPER;
import static com.tencent.supersonic.common.constant.Constants.JOIN_UNDERLINE;
import static com.tencent.supersonic.common.constant.Constants.LIMIT_UPPER;
import static com.tencent.supersonic.common.constant.Constants.ORDER_UPPER;
import static com.tencent.supersonic.common.constant.Constants.SPACE;

import com.google.common.base.Strings;
import com.tencent.supersonic.semantic.api.core.enums.TimeDimensionEnum;
import com.tencent.supersonic.semantic.api.core.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.MetricService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Slf4j
@Component
public class QueryUtils {

    private final Set<Pattern> patterns = new HashSet<>();

    @PostConstruct
    public void fillPattern() {
        Set<String> aggFunctions = new HashSet<>(Arrays.asList("MAX", "MIN", "SUM", "AVG"));
        String patternStr = "\\s*(%s\\((.*)\\)) AS";
        for (String agg : aggFunctions) {
            patterns.add(Pattern.compile(String.format(patternStr, agg)));
        }
    }

    private final MetricService metricService;
    private final DimensionService dimensionService;
    private final ParserCommandConverter parserCommandConverter;

    public QueryUtils(MetricService metricService,
            DimensionService dimensionService,
            @Lazy ParserCommandConverter parserCommandConverter) {
        this.metricService = metricService;
        this.dimensionService = dimensionService;
        this.parserCommandConverter = parserCommandConverter;
    }


    public void checkSqlParse(SqlParserResp sqlParser) {
        if (Strings.isNullOrEmpty(sqlParser.getSql()) || Strings.isNullOrEmpty(sqlParser.getSourceId())) {
            throw new RuntimeException("parse Exception: " + sqlParser.getErrMsg());
        }
    }

    public boolean isDetailQuery(QueryStructReq queryStructCmd) {
        return Objects.nonNull(queryStructCmd) && queryStructCmd.getNativeQuery() && CollectionUtils.isEmpty(
                queryStructCmd.getMetrics());
    }

    public SqlParserResp handleDetail(QueryStructReq queryStructCmd, SqlParserResp sqlParser) {
        String sqlRaw = sqlParser.getSql().trim();
        if (Strings.isNullOrEmpty(sqlRaw)) {
            throw new RuntimeException("sql is empty or null");
        }
        log.info("before handleDetail, sql:{}", sqlRaw);
        String sql = sqlRaw;

        if (isDetailQuery(queryStructCmd)) {
            String internalMetricName = parserCommandConverter.generateInternalMetricName(queryStructCmd);
            // select handle
            log.info("size:{}, metric:{}, contain:{}", queryStructCmd.getMetrics().size(), queryStructCmd.getMetrics(),
                    queryStructCmd.getMetrics().contains(internalMetricName));
            if (queryStructCmd.getMetrics().size() == 0) {
                Set<String> internalCntSet = new HashSet<>(
                        Arrays.asList(
                                String.format(", SUM(%s) AS %s", internalMetricName, internalMetricName),
                                String.format(", %s AS %s", internalMetricName, internalMetricName))
                );

                for (String target : internalCntSet) {
                    sql = sql.replace(target, SPACE);
                }
            } else {
                // dimension + metric
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(sql);
                    while (matcher.find()) {
                        String target = matcher.group(1);
                        String replace = matcher.group(2);
                        sql = sql.replace(target, replace);
                    }
                }
            }

            // group handle
            String groupTarget = "";
            if (sql.contains(GROUP_UPPER)) {
                String afterLastGroup = sql.substring(sql.lastIndexOf(GROUP_UPPER));
                log.info("afterLastGroup:{}", afterLastGroup);
                if (!Strings.isNullOrEmpty(afterLastGroup)) {
                    int tmp = afterLastGroup.length();
                    if (afterLastGroup.contains(END_SUBQUERY)) {
                        tmp = afterLastGroup.indexOf(END_SUBQUERY);
                    } else if (afterLastGroup.contains(ORDER_UPPER)) {
                        tmp = afterLastGroup.indexOf(ORDER_UPPER);
                    } else if (afterLastGroup.contains(LIMIT_UPPER)) {
                        tmp = afterLastGroup.indexOf(LIMIT_UPPER);
                    }

                    groupTarget = afterLastGroup.substring(0, tmp);
                }

                if (!Strings.isNullOrEmpty(groupTarget)) {
                    sql = sql.replace(groupTarget, SPACE);
                }
            }
            sqlParser.setSql(sql);
        }

        log.info("after handleDetail, sql:{}", sqlParser.getSql());
        return sqlParser;
    }

    public void fillItemNameInfo(QueryResultWithSchemaResp queryResultWithColumns, Long domainId) {
        List<MetricResp> metricDescList = metricService.getMetrics(domainId);
        List<DimensionResp> dimensionDescList = dimensionService.getDimensions(domainId);

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
            String nameEn = column.getNameEn();
            if (nameEn.contains(JOIN_UNDERLINE)) {
                nameEn = nameEn.split(JOIN_UNDERLINE)[1];
            }
            if (namePair.containsKey(nameEn)) {
                column.setName(namePair.get(nameEn));
            }
            if (nameTypePair.containsKey(nameEn)) {
                column.setShowType(nameTypePair.get(nameEn));
            }
        });
    }

    public void fillItemNameInfo(QueryResultWithSchemaResp queryResultWithColumns,
            QueryMultiStructReq queryMultiStructCmd) {
        List<Aggregator> aggregators = queryMultiStructCmd.getQueryStructCmds().stream()
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
            String nameEn = column.getNameEn();
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
}
