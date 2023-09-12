package com.tencent.supersonic.semantic.query.utils;

import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryDslReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Value("${dimension.value.map.sql.enable:true}")
    private Boolean dimensionValueMapSqlEnable;
    @Autowired
    private DimensionService dimensionService;

    @Around("execution(* com.tencent.supersonic.semantic.query.service.QueryServiceImpl.queryBySql(..))")
    public Object handleSqlDimValue(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!dimensionValueMapSqlEnable) {
            log.debug("sql dimensionValueMapEnable is false, skip dimensionValueMap");
            QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) joinPoint.proceed();
            return queryResultWithColumns;
        }
        Object[] args = joinPoint.getArgs();
        QueryDslReq queryDslReq = (QueryDslReq) args[0];

        List<DimensionResp> dimensions = dimensionService.getDimensions(queryDslReq.getModelId());
        Map<String, Map<String, String>> techNameToBizName = getTechNameToBizName(dimensions);

        QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) joinPoint.proceed();
        if (Objects.nonNull(queryResultWithColumns)) {
            rewriteDimValue(queryResultWithColumns, techNameToBizName);
        }
        return queryResultWithColumns;
    }


    @Around("execution(* com.tencent.supersonic.semantic.query.rest.QueryController.queryByStruct(..))"
            + " || execution(* com.tencent.supersonic.semantic.query.service.QueryService.queryByStruct(..))"
            + " || execution(* com.tencent.supersonic.semantic.query.service.QueryService.queryByStructWithAuth(..))")
    public Object handleDimValue(ProceedingJoinPoint joinPoint) throws Throwable {

        if (!dimensionValueMapEnable) {
            log.debug("dimensionValueMapEnable is false, skip dimensionValueMap");
            QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) joinPoint.proceed();
            return queryResultWithColumns;
        }

        Object[] args = joinPoint.getArgs();
        QueryStructReq queryStructReq = (QueryStructReq) args[0];
        Long modelId = queryStructReq.getModelId();

        List<DimensionResp> dimensions = dimensionService.getDimensions(modelId);
        Map<String, Map<String, String>> dimAndAliasAndTechNamePair = getAliasAndBizNameToTechName(dimensions);
        Map<String, Map<String, String>> dimAndTechNameAndBizNamePair = getTechNameToBizName(dimensions);

        rewriteFilter(queryStructReq.getDimensionFilters(), dimAndAliasAndTechNamePair);

        QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) joinPoint.proceed();
        if (Objects.nonNull(queryResultWithColumns)) {
            rewriteDimValue(queryResultWithColumns, dimAndTechNameAndBizNamePair);
        }

        return queryResultWithColumns;
    }

    private void rewriteDimValue(QueryResultWithSchemaResp queryResultWithColumns,
            Map<String, Map<String, String>> dimAndTechNameAndBizNamePair) {
        if (!selectDimValueMap(queryResultWithColumns.getColumns(), dimAndTechNameAndBizNamePair)) {
            return;
        }
        log.debug("start rewriteDimValue for resultList");
        for (Map<String, Object> line : queryResultWithColumns.getResultList()) {
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
