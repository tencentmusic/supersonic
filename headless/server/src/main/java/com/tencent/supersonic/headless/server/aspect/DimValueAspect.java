package com.tencent.supersonic.headless.server.aspect;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.DimensionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Aspect
@Component
@Slf4j
public class DimValueAspect {

    @Value("${dimension.value.map.enable:true}")
    private Boolean dimensionValueMapEnable;
    @Autowired
    private DimensionService dimensionService;

    @Around("execution(* com.tencent.supersonic.headless.server.service.QueryService.queryByReq(..))")
    public Object handleDimValue(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!dimensionValueMapEnable) {
            log.debug("dimensionValueMapEnable is false, skip dimensionValueMap");
            SemanticQueryResp queryResultWithColumns = (SemanticQueryResp) joinPoint.proceed();
            return queryResultWithColumns;
        }

        Object[] args = joinPoint.getArgs();
        SemanticQueryReq queryReq = (SemanticQueryReq) args[0];
        if (queryReq instanceof QueryStructReq) {
            return handleStructDimValue(joinPoint);
        }
        if (queryReq instanceof QuerySqlReq) {
            return handleSqlDimValue(joinPoint);
        }
        throw new InvalidArgumentException("queryReq is not Invalid:" + queryReq);
    }

    private SemanticQueryResp handleStructDimValue(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        QueryStructReq queryStructReq = (QueryStructReq) args[0];
        MetaFilter metaFilter = new MetaFilter(Lists.newArrayList(queryStructReq.getModelIds()));
        List<DimensionResp> dimensions = dimensionService.getDimensions(metaFilter);
        Map<String, Map<String, String>> dimAndAliasAndTechNamePair = getAliasAndBizNameToTechName(dimensions);
        Map<String, Map<String, String>> dimAndTechNameAndBizNamePair = getTechNameToBizName(dimensions);

        rewriteFilter(queryStructReq.getDimensionFilters(), dimAndAliasAndTechNamePair);

        SemanticQueryResp semanticQueryResp = (SemanticQueryResp) joinPoint.proceed();
        if (Objects.nonNull(semanticQueryResp)) {
            rewriteDimValue(semanticQueryResp, dimAndTechNameAndBizNamePair);
        }

        return semanticQueryResp;
    }

    public Object handleSqlDimValue(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        QuerySqlReq querySqlReq = (QuerySqlReq) args[0];
        MetaFilter metaFilter = new MetaFilter(Lists.newArrayList(querySqlReq.getModelIds()));
        String sql = querySqlReq.getSql();
        log.info("correctorSql before replacing:{}", sql);
        List<FieldExpression> fieldExpressionList = SqlSelectHelper.getWhereExpressions(sql);
        List<DimensionResp> dimensions = dimensionService.getDimensions(metaFilter);
        Set<String> fieldNames = dimensions.stream().map(SchemaItem::getName).collect(Collectors.toSet());
        Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
        for (FieldExpression expression : fieldExpressionList) {
            if (!fieldNames.contains(expression.getFieldName())) {
                continue;
            }
            for (DimensionResp dimension : dimensions) {
                if (!expression.getFieldName().equals(dimension.getName())
                        || CollectionUtils.isEmpty(dimension.getDimValueMaps())) {
                    continue;
                }
                // consider '=' filter
                if (expression.getOperator().equals(FilterOperatorEnum.EQUALS.getValue())) {
                    dimension.getDimValueMaps().stream().forEach(dimValue -> {
                        if (!CollectionUtils.isEmpty(dimValue.getAlias())
                                && dimValue.getAlias().contains(expression.getFieldValue().toString())) {
                            getFiledNameToValueMap(filedNameToValueMap, expression.getFieldValue().toString(),
                                    dimValue.getTechName(), expression.getFieldName());
                        }
                    });
                }
                // consider 'in' filter,each element needs to judge.
                replaceInCondition(expression, dimension, filedNameToValueMap);
            }
        }
        sql = SqlReplaceHelper.replaceValue(sql, filedNameToValueMap);
        log.info("correctorSql after replacing:{}", sql);
        querySqlReq.setSql(sql);
        Map<String, Map<String, String>> techNameToBizName = getTechNameToBizName(dimensions);

        SemanticQueryResp queryResultWithColumns = (SemanticQueryResp) joinPoint.proceed();
        if (Objects.nonNull(queryResultWithColumns)) {
            rewriteDimValue(queryResultWithColumns, techNameToBizName);
        }
        return queryResultWithColumns;
    }

    public void replaceInCondition(FieldExpression expression, DimensionResp dimension,
            Map<String, Map<String, String>> filedNameToValueMap) {
        if (expression.getOperator().equals(FilterOperatorEnum.IN.getValue())) {
            String fieldValue = JsonUtil.toString(expression.getFieldValue());
            fieldValue = fieldValue.replace("'", "");
            List<String> values = JsonUtil.toList(fieldValue, String.class);
            List<String> revisedValues = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                Boolean flag = new Boolean(false);
                for (DimValueMap dimValueMap : dimension.getDimValueMaps()) {
                    if (!CollectionUtils.isEmpty(dimValueMap.getAlias())
                            && dimValueMap.getAlias().contains(values.get(i))) {
                        flag = true;
                        revisedValues.add(dimValueMap.getTechName());
                        break;
                    }
                }
                if (!flag) {
                    revisedValues.add(values.get(i));
                }
            }
            if (!revisedValues.equals(values)) {
                getFiledNameToValueMap(filedNameToValueMap, JsonUtil.toString(values),
                        JsonUtil.toString(revisedValues), expression.getFieldName());
            }
        }
    }

    public void getFiledNameToValueMap(Map<String, Map<String, String>> filedNameToValueMap,
            String oldValue, String newValue, String fieldName) {
        Map<String, String> map = new HashMap<>();
        map.put(oldValue, newValue);
        filedNameToValueMap.put(fieldName, map);
    }

    private void rewriteDimValue(SemanticQueryResp semanticQueryResp,
            Map<String, Map<String, String>> dimAndTechNameAndBizNamePair) {
        if (!selectDimValueMap(semanticQueryResp.getColumns(), dimAndTechNameAndBizNamePair)) {
            return;
        }
        log.debug("start rewriteDimValue for resultList");
        for (Map<String, Object> line : semanticQueryResp.getResultList()) {
            for (String bizName : line.keySet()) {
                if (dimAndTechNameAndBizNamePair.containsKey(bizName) && Objects.nonNull(line.get(bizName))) {
                    String techName = line.get(bizName).toString();
                    Map<String, String> techAndBizPair = dimAndTechNameAndBizNamePair.get(bizName);
                    if (!CollectionUtils.isEmpty(techAndBizPair) && techAndBizPair.containsKey(techName)) {
                        String bizValueName = techAndBizPair.get(techName);
                        if (Strings.isNotEmpty(bizValueName)) {
                            line.put(bizName, bizValueName);
                        }
                    }
                }
            }
        }
    }

    private boolean selectDimValueMap(List<QueryColumn> columns, Map<String,
            Map<String, String>> dimAndTechNameAndBizNamePair) {
        if (CollectionUtils.isEmpty(dimAndTechNameAndBizNamePair)
                || CollectionUtils.isEmpty(dimAndTechNameAndBizNamePair)) {
            return false;
        }

        for (QueryColumn queryColumn : columns) {
            if (dimAndTechNameAndBizNamePair.containsKey(queryColumn.getNameEn())) {
                return true;
            }
        }
        return false;
    }

    private void rewriteFilter(List<Filter> dimensionFilters, Map<String, Map<String, String>> aliasAndTechNamePair) {
        for (Filter filter : dimensionFilters) {
            if (Objects.isNull(filter)) {
                continue;
            }

            if (CollectionUtils.isEmpty(filter.getChildren())) {
                Object value = filter.getValue();
                String bizName = filter.getBizName();
                if (aliasAndTechNamePair.containsKey(bizName)) {
                    Map<String, String> aliasPair = aliasAndTechNamePair.get(bizName);
                    if (Objects.nonNull(value)) {
                        if (value instanceof List) {
                            List<String> values = (List) value;
                            List<String> valuesNew = new ArrayList<>();
                            for (String valueSingle : values) {
                                if (aliasPair.containsKey(valueSingle)) {
                                    valuesNew.add(aliasPair.get(valueSingle));
                                } else {
                                    valuesNew.add(valueSingle);
                                }
                            }
                            filter.setValue(valuesNew);
                        }
                        if (value instanceof String) {
                            if (aliasPair.containsKey(value)) {
                                filter.setValue(aliasPair.get(value));
                            }
                        }
                    }
                }
                return;
            }

            rewriteFilter(filter.getChildren(), aliasAndTechNamePair);
        }
    }

    private Map<String, Map<String, String>> getAliasAndBizNameToTechName(List<DimensionResp> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return new HashMap<>();
        }
        Map<String, Map<String, String>> result = new HashMap<>();
        for (DimensionResp dimension : dimensions) {
            if (needSkipDimension(dimension)) {
                continue;
            }
            String bizName = dimension.getBizName();
            List<DimValueMap> dimValueMaps = dimension.getDimValueMaps();
            Map<String, String> aliasAndBizNameToTechName = new HashMap<>();

            for (DimValueMap dimValueMap : dimValueMaps) {
                if (needSkipDimValue(dimValueMap)) {
                    continue;
                }
                if (Strings.isNotEmpty(dimValueMap.getBizName())) {
                    aliasAndBizNameToTechName.put(dimValueMap.getBizName(), dimValueMap.getTechName());
                }
                if (!CollectionUtils.isEmpty(dimValueMap.getAlias())) {
                    dimValueMap.getAlias().stream().forEach(alias -> {
                        if (Strings.isNotEmpty(alias)) {
                            aliasAndBizNameToTechName.put(alias, dimValueMap.getTechName());
                        }
                    });
                }
            }

            if (!CollectionUtils.isEmpty(aliasAndBizNameToTechName)) {
                result.put(bizName, aliasAndBizNameToTechName);
            }
        }
        return result;
    }

    private boolean needSkipDimValue(DimValueMap dimValueMap) {
        return Objects.isNull(dimValueMap) || Strings.isEmpty(dimValueMap.getTechName());
    }

    private Map<String, Map<String, String>> getTechNameToBizName(List<DimensionResp> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return new HashMap<>();
        }
        Map<String, Map<String, String>> result = new HashMap<>();
        for (DimensionResp dimension : dimensions) {
            if (needSkipDimension(dimension)) {
                continue;
            }
            String bizName = dimension.getBizName();
            List<DimValueMap> dimValueMaps = dimension.getDimValueMaps();
            Map<String, String> techNameToBizName = new HashMap<>();

            for (DimValueMap dimValueMap : dimValueMaps) {
                if (needSkipDimValue(dimValueMap)) {
                    continue;
                }
                if (StringUtils.isNotEmpty(dimValueMap.getBizName())) {
                    techNameToBizName.put(dimValueMap.getTechName(), dimValueMap.getBizName());
                }
            }

            if (!CollectionUtils.isEmpty(techNameToBizName)) {
                result.put(bizName, techNameToBizName);
            }
        }
        return result;
    }

    private boolean needSkipDimension(DimensionResp dimension) {
        return Objects.isNull(dimension) || Strings.isEmpty(dimension.getBizName()) || CollectionUtils.isEmpty(
                dimension.getDimValueMaps());
    }
}
