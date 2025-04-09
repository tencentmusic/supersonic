package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.headless.api.pojo.enums.SemanticType;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.response.*;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.UNIONALL;

@Slf4j
@Component
public class QueryUtils {

    private static final String pattern = "\\(`(.*?)`\\)";

    private static final String no_quotation_pattern = "\\((.*?)\\)";

    @Value("${s2.query-optimizer.enable:true}")
    private Boolean optimizeEnable;

    public void populateQueryColumns(SemanticQueryResp semanticQueryResp,
            SemanticSchemaResp semanticSchemaResp) {
        Map<String, MetricResp> metricRespMap = createMetricRespMap(semanticSchemaResp);
        Map<String, DimensionResp> dimensionRespMap = createDimRespMap(semanticSchemaResp);
        Map<String, String> namePair = new HashMap<>();
        Map<String, String> nameTypePair = new HashMap<>();
        populateNamePairs(semanticSchemaResp, namePair, nameTypePair);
        List<QueryColumn> columns = semanticQueryResp.getColumns();
        columns.forEach(column -> processColumn(column, namePair, nameTypePair, metricRespMap,
                dimensionRespMap));
    }

    private Map<String, MetricResp> createMetricRespMap(SemanticSchemaResp semanticSchemaResp) {
        List<MetricSchemaResp> metrics = semanticSchemaResp.getMetrics();
        return metrics.stream()
                .collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
    }

    private Map<String, DimensionResp> createDimRespMap(SemanticSchemaResp semanticSchemaResp) {
        List<DimSchemaResp> dimensions = semanticSchemaResp.getDimensions();
        return dimensions.stream()
                .collect(Collectors.toMap(DimensionResp::getBizName, a -> a, (k1, k2) -> k1));
    }

    private void populateNamePairs(SemanticSchemaResp semanticSchemaResp,
            Map<String, String> namePair, Map<String, String> nameTypePair) {
        semanticSchemaResp.getMetrics().forEach(metricDesc -> {
            namePair.put(metricDesc.getBizName(), metricDesc.getName());
            nameTypePair.put(metricDesc.getBizName(), SemanticType.NUMBER.name());
        });
        semanticSchemaResp.getDimensions().forEach(dimensionDesc -> {
            namePair.put(dimensionDesc.getBizName(), dimensionDesc.getName());
            nameTypePair.put(dimensionDesc.getBizName(), dimensionDesc.getSemanticType());
        });
    }

    private Map<String, String> reverseMap(Map<String, String> namePair) {
        Map<String, String> reverseNamePair = new HashMap<>();
        namePair.forEach((key, value) -> {
            reverseNamePair.put(value, key);
        });
        return reverseNamePair;
    }

    private void processColumn(QueryColumn column, Map<String, String> namePair,
            Map<String, String> nameTypePair, Map<String, MetricResp> metricRespMap,
            Map<String, DimensionResp> dimensionRespMap) {
        String nameEn = getName(column.getBizName());
        if (namePair.containsKey(nameEn)) {
            column.setName(namePair.get(nameEn));
        } else if (namePair.containsValue(nameEn)) {
            Map<String, String> reverseMap = reverseMap(namePair);
            nameEn = reverseMap.get(nameEn);
        } else {
            String nameEnByRegex = getNameEnByRegex(nameEn, pattern);
            if (StringUtils.isEmpty(nameEnByRegex)) {
                nameEnByRegex = getNameEnByRegex(nameEn, no_quotation_pattern);
            }
            if (StringUtils.isNotEmpty(nameEnByRegex)
                    && StringUtils.isNotEmpty(namePair.get(nameEnByRegex))) {
                String filedName = namePair.get(nameEnByRegex);
                column.setName(nameEn.replaceAll(nameEnByRegex, filedName));
            }
        }
        // 使用 final 临时变量
        final String finalNameEn = nameEn;
        // set showType
        if (nameTypePair.containsKey(finalNameEn)) {
            column.setShowType(nameTypePair.get(finalNameEn));
        }
        if (!nameTypePair.containsKey(finalNameEn) && isNumberType(column.getType())) {
            column.setShowType(SemanticType.NUMBER.name());
        }
        if (StringUtils.isEmpty(column.getShowType())) {
            column.setShowType(SemanticType.CATEGORY.name());
        }
        // set dataFormat/dataFormatType
        if (metricRespMap.containsKey(finalNameEn)) {
            column.setDataFormatType(metricRespMap.get(finalNameEn).getDataFormatType());
            column.setDataFormat(metricRespMap.get(finalNameEn).getDataFormat());
            column.setModelId(metricRespMap.get(finalNameEn).getModelId());
        } else {
            // if column nameEn contains metric name, use metric dataFormatType
            metricRespMap.values().forEach(metric -> {
                if (finalNameEn.contains(metric.getName())
                        || finalNameEn.contains(metric.getBizName())) {
                    column.setDataFormatType(metric.getDataFormatType());
                    column.setDataFormat(metric.getDataFormat());
                    column.setModelId(metric.getModelId());
                }
                // if column nameEn contains metric alias, use metric dataFormatType
                if (column.getDataFormatType() == null && metric.getAlias() != null) {
                    for (String alias : metric.getAlias().split(",")) {
                        if (finalNameEn.contains(alias)) {
                            column.setDataFormatType(metric.getDataFormatType());
                            column.setDataFormat(metric.getDataFormat());
                            column.setModelId(metric.getModelId());
                            break;
                        }
                    }
                }
            });
        }

        if (dimensionRespMap.containsKey(finalNameEn)) {
            column.setModelId(dimensionRespMap.get(finalNameEn).getModelId());
        }
        // set name by NameEn
        if (StringUtils.isBlank(column.getName()) && StringUtils.isNotBlank(column.getBizName())) {
            column.setName(column.getBizName());
        }
    }

    private boolean isNumberType(String type) {
        if (StringUtils.isBlank(type)) {
            return false;
        }
        return type.equalsIgnoreCase("int") || type.equalsIgnoreCase("bigint")
                || type.equalsIgnoreCase("tinyint") || type.equalsIgnoreCase("smallint")
                || type.equalsIgnoreCase("float") || type.equalsIgnoreCase("double")
                || type.equalsIgnoreCase("real") || type.equalsIgnoreCase("numeric")
                || type.toLowerCase().startsWith("decimal") || type.toLowerCase().startsWith("uint")
                || type.toLowerCase().startsWith("int")
                || type.toLowerCase().equalsIgnoreCase("decfloat");
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

    public QueryStatement unionAll(QueryMultiStructReq queryMultiStructCmd,
            List<QueryStatement> queryStatements) {
        QueryStatement sqlParser = new QueryStatement();
        StringBuilder unionSqlBuilder = new StringBuilder();
        for (int i = 0; i < queryStatements.size(); i++) {
            String selectStr = SqlGenerateUtils
                    .getUnionSelect(queryMultiStructCmd.getQueryStructReqs().get(i));
            unionSqlBuilder.append(String.format("select %s from ( %s ) sub_sql_%s", selectStr,
                    queryStatements.get(i).getSql(), i));
            unionSqlBuilder.append(UNIONALL);
        }
        String unionSql = unionSqlBuilder.substring(0,
                unionSqlBuilder.length() - Constants.UNIONALL.length());
        sqlParser.setSql(unionSql);
        log.info("union sql parser:{}", sqlParser);
        return sqlParser;
    }

    public Boolean enableOptimize() {
        return optimizeEnable;
    }
}
