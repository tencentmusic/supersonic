package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.enums.SemanticType;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.JOIN_UNDERLINE;
import static com.tencent.supersonic.common.pojo.Constants.UNIONALL;


@Slf4j
@Component
public class QueryUtils {

    private static final String pattern = "\\(`(.*?)`\\)";

    private static final String no_quotation_pattern = "\\((.*?)\\)";

    private final Set<Pattern> patterns = new HashSet<>();

    @Value("${s2.query-optimizer.enable:true}")
    private Boolean optimizeEnable;

    @PostConstruct
    public void fillPattern() {
        Set<String> aggFunctions = new HashSet<>(Arrays.asList("MAX", "MIN", "SUM", "AVG"));
        String patternStr = "\\s*(%s\\((.*)\\)) AS";
        for (String agg : aggFunctions) {
            patterns.add(Pattern.compile(String.format(patternStr, agg)));
        }
    }

    public void fillItemNameInfo(SemanticQueryResp semanticQueryResp, SemanticSchemaResp semanticSchemaResp) {
        List<MetricSchemaResp> metricDescList = semanticSchemaResp.getMetrics();
        List<DimSchemaResp> dimSchemaResps = semanticSchemaResp.getDimensions();
        Map<String, MetricResp> metricRespMap =
                metricDescList.stream().collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
        Map<String, String> namePair = new HashMap<>();
        Map<String, String> nameTypePair = new HashMap<>();
        addSysTimeDimension(namePair, nameTypePair);
        metricDescList.forEach(metricDesc -> {
            namePair.put(metricDesc.getBizName(), metricDesc.getName());
            nameTypePair.put(metricDesc.getBizName(), SemanticType.NUMBER.name());
        });
        dimSchemaResps.forEach(dimensionDesc -> {
            namePair.put(dimensionDesc.getBizName(), dimensionDesc.getName());
            nameTypePair.put(dimensionDesc.getBizName(), dimensionDesc.getSemanticType());
        });
        List<QueryColumn> columns = semanticQueryResp.getColumns();
        columns.forEach(column -> {
            String nameEn = getName(column.getNameEn().toLowerCase());
            if (nameEn.contains(JOIN_UNDERLINE)) {
                nameEn = nameEn.split(JOIN_UNDERLINE)[1];
            }
            if (namePair.containsKey(nameEn)) {
                column.setName(namePair.get(nameEn));
            } else {
                String nameEnByRegex = getNameEnByRegex(nameEn, pattern);
                if (StringUtils.isEmpty(nameEnByRegex)) {
                    nameEnByRegex = getNameEnByRegex(nameEn, no_quotation_pattern);
                }
                if (StringUtils.isNotEmpty(nameEnByRegex) && StringUtils.isNotEmpty(namePair.get(nameEnByRegex))) {
                    String filedName = namePair.get(nameEnByRegex);
                    column.setName(nameEn.replaceAll(nameEnByRegex, filedName));
                }
            }
            if (nameTypePair.containsKey(nameEn)) {
                column.setShowType(nameTypePair.get(nameEn));
            }
            if (!nameTypePair.containsKey(nameEn) && isNumberType(column.getType())) {
                column.setShowType(SemanticType.NUMBER.name());
            }
            if (metricRespMap.containsKey(nameEn)) {
                column.setDataFormatType(metricRespMap.get(nameEn).getDataFormatType());
                column.setDataFormat(metricRespMap.get(nameEn).getDataFormat());
            }
            if (StringUtils.isEmpty(column.getShowType())) {
                column.setShowType(SemanticType.CATEGORY.name());
            }
        });
    }

    private String getName(String nameEn) {
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(nameEn);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("`", "");
        }
        return nameEn;
    }

    private String getNameEnByRegex(String nameEn, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(nameEn);

        if (m.find()) {
            String result = m.group(1);
            return result;
        }
        return null;
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

    private static void addSysTimeDimension(Map<String, String> namePair, Map<String, String> nameTypePair) {
        for (TimeDimensionEnum timeDimensionEnum : TimeDimensionEnum.values()) {
            namePair.put(timeDimensionEnum.getName(), "date");
            nameTypePair.put(timeDimensionEnum.getName(), "DATE");
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

    public Boolean enableOptimize() {
        return optimizeEnable;
    }
}
