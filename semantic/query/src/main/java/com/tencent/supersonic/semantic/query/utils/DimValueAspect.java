package com.tencent.supersonic.semantic.query.utils;

import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
public class DimValueAspect {

    @Value("${dimension.value.map.enable:true}")
    private Boolean dimensionValueMapEnable;

    @Autowired
    private DimensionService dimensionService;

    @Around("execution(* com.tencent.supersonic.semantic.query.rest.QueryController.queryByStruct(..))" +
            " || execution(* com.tencent.supersonic.semantic.query.service.QueryService.queryByStruct(..))")
    public Object handleDimValue(ProceedingJoinPoint joinPoint) throws Throwable {

        if (!dimensionValueMapEnable) {
            log.debug("dimensionValueMapEnable is false, skip dimensionValueMap");
            QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) joinPoint.proceed();
            return queryResultWithColumns;
        }

        Object[] args = joinPoint.getArgs();
        QueryStructReq queryStructReq = (QueryStructReq) args[0];
        Long domainId = queryStructReq.getDomainId();

        List<DimensionResp> dimensions = dimensionService.getDimensions(domainId);
        Map<String, Map<String, String>> dimAndAliasAndTechNamePair = new ConcurrentHashMap<>();
        Map<String, Map<String, String>> dimAndTechNameAndBizNamePair = new ConcurrentHashMap<>();
        generateAliasAndTechNamePair(dimensions, dimAndAliasAndTechNamePair, dimAndTechNameAndBizNamePair);

        rewriteFilter(queryStructReq.getDimensionFilters(), dimAndAliasAndTechNamePair);

        QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) joinPoint.proceed();
        if (Objects.nonNull(queryResultWithColumns)) {
            rewriteDimValue(queryResultWithColumns, dimAndTechNameAndBizNamePair);
        }

        return queryResultWithColumns;
    }

    private void rewriteDimValue(QueryResultWithSchemaResp queryResultWithColumns, Map<String, Map<String, String>> dimAndTechNameAndBizNamePair) {
        if (!selectDimValueMap(queryResultWithColumns.getColumns(), dimAndTechNameAndBizNamePair)) {
            return;
        }
        log.debug("start rewriteDimValue for resultList");
        for (Map<String, Object> line : queryResultWithColumns.getResultList()) {
            for (String bizName : line.keySet()) {
                String techName = line.get(bizName).toString();
                if (dimAndTechNameAndBizNamePair.containsKey(bizName)) {
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

    private boolean selectDimValueMap(List<QueryColumn> columns, Map<String, Map<String, String>> dimAndTechNameAndBizNamePair) {
        if (CollectionUtils.isEmpty(dimAndTechNameAndBizNamePair) || CollectionUtils.isEmpty(dimAndTechNameAndBizNamePair)) {
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
                                boolean f = aliasPair.containsKey(valueSingle) ? valuesNew.add(aliasPair.get(valueSingle)) : valuesNew.add(valueSingle);
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

    private void generateAliasAndTechNamePair(List<DimensionResp> dimensions
            , Map<String, Map<String, String>> dimAndAliasAndTechNamePair
            , Map<String, Map<String, String>> dimAndTechNameAndBizNamePair) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        dimensions.stream().forEach(dimension -> {
            if (Objects.nonNull(dimension) && Strings.isNotEmpty(dimension.getBizName())
                    && !CollectionUtils.isEmpty(dimension.getDimValueMaps())) {
                String bizName = dimension.getBizName();

                List<DimValueMap> dimValueMaps = dimension.getDimValueMaps();
                Map<String, String> innerPairTech = new HashMap<>();
                Map<String, String> innerPairBiz = new HashMap<>();

                dimValueMaps.stream().forEach(dimValueMap -> {
                    if (Objects.nonNull(dimValueMap) && !CollectionUtils.isEmpty(dimValueMap.getAlias())
                            && Strings.isNotEmpty(dimValueMap.getTechName())) {

                        // add bizName and techName pair
                        if (Strings.isNotEmpty(dimValueMap.getBizName())) {
                            innerPairTech.put(dimValueMap.getBizName(), dimValueMap.getTechName());
                        }

                        dimValueMap.getAlias().stream().forEach(alias -> {
                            if (Strings.isNotEmpty(alias)) {
                                innerPairTech.put(alias, dimValueMap.getTechName());
                            }
                        });
                    }
                    if (Objects.nonNull(dimValueMap) && Strings.isNotEmpty(dimValueMap.getTechName())) {
                        innerPairBiz.put(dimValueMap.getTechName(), dimValueMap.getBizName());
                    }
                });

                if (!CollectionUtils.isEmpty(innerPairTech)) {
                    dimAndAliasAndTechNamePair.put(bizName, innerPairTech);
                }

                if (!CollectionUtils.isEmpty(innerPairBiz)) {
                    dimAndTechNameAndBizNamePair.put(bizName, innerPairBiz);
                }
            }
        });

    }
}