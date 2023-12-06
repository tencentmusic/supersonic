package com.tencent.supersonic.semantic.query.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthResGrp;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryAuthorization;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthCommonService {
    private static final ObjectMapper MAPPER = new ObjectMapper().setDateFormat(
            new SimpleDateFormat(Constants.DAY_FORMAT));
    @Autowired
    private AuthService authService;
    @Autowired
    private DimensionService dimensionService;
    @Autowired
    private MetricService metricService;

    @Autowired
    private ModelService modelService;

    public boolean doModelAdmin(User user, List<Long> modelIds) {
        List<ModelResp> modelListAdmin = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        if (CollectionUtils.isEmpty(modelListAdmin)) {
            return false;
        } else {
            Set<Long> modelAdmins = modelListAdmin.stream().map(ModelResp::getId).collect(Collectors.toSet());
            return !CollectionUtils.isEmpty(modelAdmins) && modelAdmins.containsAll(modelIds);
        }
    }

    public void doModelVisible(User user, List<Long> modelIds) {
        Boolean visible = true;
        List<ModelResp> modelListVisible = modelService.getModelListWithAuth(user, null, AuthType.VISIBLE);
        if (CollectionUtils.isEmpty(modelListVisible)) {
            visible = false;
        } else {
            Set<Long> modelVisibles = modelListVisible.stream().map(ModelResp::getId).collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(modelVisibles) && !modelVisibles.containsAll(modelIds)) {
                visible = false;
            }
        }
        if (!visible) {
            ModelResp modelResp = modelService.getModel(modelIds.get(0));
            String modelName = modelResp.getName();
            List<String> admins = modelService.getModelAdmin(modelResp.getId());
            String message = String.format("您没有模型[%s]权限，请联系管理员%s开通", modelName, admins);
            throw new InvalidPermissionException(message);
        }

    }

    public Set<String> getHighSensitiveColsByModelId(List<Long> modelIds) {
        Set<String> highSensitiveCols = new HashSet<>();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setModelIds(modelIds);
        metaFilter.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        List<DimensionResp> highSensitiveDimensions = dimensionService.getDimensions(metaFilter);
        List<MetricResp> highSensitiveMetrics = metricService.getMetrics(metaFilter);
        if (!CollectionUtils.isEmpty(highSensitiveDimensions)) {
            highSensitiveDimensions.forEach(dim -> highSensitiveCols.add(dim.getBizName()));
        }
        if (!CollectionUtils.isEmpty(highSensitiveMetrics)) {
            highSensitiveMetrics.forEach(metric -> highSensitiveCols.add(metric.getBizName()));
        }
        return highSensitiveCols;
    }

    public AuthorizedResourceResp getAuthorizedResource(User user, List<Long> modelIds,
                                                           Set<String> sensitiveResReq) {
        List<AuthRes> resourceReqList = new ArrayList<>();
        sensitiveResReq.forEach(res -> resourceReqList.add(new AuthRes(modelIds.get(0), res)));
        QueryAuthResReq queryAuthResReq = new QueryAuthResReq();
        queryAuthResReq.setResources(resourceReqList);
        queryAuthResReq.setModelIds(modelIds);
        AuthorizedResourceResp authorizedResource = fetchAuthRes(queryAuthResReq, user);
        log.info("user:{}, domainId:{}, after queryAuthorizedResources:{}", user.getName(), modelIds,
                authorizedResource);
        return authorizedResource;
    }

    private AuthorizedResourceResp fetchAuthRes(QueryAuthResReq queryAuthResReq, User user) {
        log.info("queryAuthResReq:{}", queryAuthResReq);
        return authService.queryAuthorizedResources(queryAuthResReq, user);
    }

    public Set<String> getAuthResNameSet(AuthorizedResourceResp authorizedResource, List<Long> modelIds) {
        Set<String> resAuthName = new HashSet<>();
        List<AuthResGrp> authResGrpList = authorizedResource.getResources();
        authResGrpList.stream().forEach(authResGrp -> {
            List<AuthRes> cols = authResGrp.getGroup();
            if (!CollectionUtils.isEmpty(cols)) {
                cols.stream().filter(col -> modelIds.contains(col.getModelId()))
                        .forEach(col -> resAuthName.add(col.getName()));
            }

        });
        log.info("resAuthName:{}", resAuthName);
        return resAuthName;
    }

    public boolean allSensitiveResReqIsOk(Set<String> sensitiveResReq, Set<String> resAuthSet) {
        if (resAuthSet.containsAll(sensitiveResReq)) {
            return true;
        }
        log.info("sensitiveResReq:{}, resAuthSet:{}", sensitiveResReq, resAuthSet);
        return false;
    }

    public QueryResultWithSchemaResp getQueryResultWithColumns(QueryResultWithSchemaResp resultWithColumns,
                                                               List<Long> modelIds,
                                                               AuthorizedResourceResp authResource) {
        addPromptInfoInfo(modelIds, resultWithColumns, authResource, Sets.newHashSet());
        return resultWithColumns;
    }

    public QueryResultWithSchemaResp desensitizationData(QueryResultWithSchemaResp raw, Set<String> need2Apply) {
        log.debug("start desensitizationData logic");
        if (CollectionUtils.isEmpty(need2Apply)) {
            log.info("user has all sensitiveRes");
            return raw;
        }

        List<QueryColumn> columns = raw.getColumns();

        boolean doDesensitization = false;
        for (QueryColumn queryColumn : columns) {
            for (String sensitiveCol : need2Apply) {
                if (queryColumn.getNameEn().contains(sensitiveCol)) {
                    doDesensitization = true;
                    break;
                }
            }
        }
        if (!doDesensitization) {
            return raw;
        }

        QueryResultWithSchemaResp queryResultWithColumns = raw;
        try {
            queryResultWithColumns = deepCopyResult(raw);
        } catch (Exception e) {
            log.warn("deepCopyResult: ", e);
        }
        addAuthorizedSchemaInfo(queryResultWithColumns.getColumns(), need2Apply);
        desensitizationInternal(queryResultWithColumns.getResultList(), need2Apply);
        return queryResultWithColumns;
    }

    private void addAuthorizedSchemaInfo(List<QueryColumn> columns, Set<String> need2Apply) {
        if (CollectionUtils.isEmpty(need2Apply)) {
            return;
        }
        columns.stream().forEach(col -> {
            if (need2Apply.contains(col.getNameEn())) {
                col.setAuthorized(false);
            }
        });
    }

    private void desensitizationInternal(List<Map<String, Object>> result, Set<String> need2Apply) {
        log.info("start desensitizationInternal logic");
        for (int i = 0; i < result.size(); i++) {
            Map<String, Object> row = result.get(i);
            Map<String, Object> newRow = new HashMap<>();
            for (String col : row.keySet()) {
                boolean sensitive = false;
                for (String sensitiveCol : need2Apply) {
                    if (col.contains(sensitiveCol)) {
                        sensitive = true;
                        break;
                    }
                }
                if (sensitive) {
                    newRow.put(col, "******");
                } else {
                    newRow.put(col, row.get(col));
                }
            }
            result.set(i, newRow);
        }
    }

    private QueryResultWithSchemaResp deepCopyResult(QueryResultWithSchemaResp raw) throws Exception {
        QueryResultWithSchemaResp queryResultWithColumns = new QueryResultWithSchemaResp();
        BeanUtils.copyProperties(raw, queryResultWithColumns);

        List<QueryColumn> columns = new ArrayList<>();
        if (!CollectionUtils.isEmpty(raw.getColumns())) {
            String columnsStr = MAPPER.writeValueAsString(raw.getColumns());
            columns = MAPPER.readValue(columnsStr, new TypeReference<List<QueryColumn>>() {
            });
            queryResultWithColumns.setColumns(columns);
        }
        queryResultWithColumns.setColumns(columns);

        List<Map<String, Object>> resultData = new ArrayList<>();
        if (!CollectionUtils.isEmpty(raw.getResultList())) {
            for (Map<String, Object> line : raw.getResultList()) {
                Map<String, Object> newLine = new HashMap<>();
                newLine.putAll(line);
                resultData.add(newLine);
            }
        }
        queryResultWithColumns.setResultList(resultData);
        return queryResultWithColumns;
    }

    public void addPromptInfoInfo(List<Long> modelIds, QueryResultWithSchemaResp queryResultWithColumns,
                                     AuthorizedResourceResp authorizedResource, Set<String> need2Apply) {
        List<DimensionFilter> filters = authorizedResource.getFilters();
        if (CollectionUtils.isEmpty(need2Apply) && CollectionUtils.isEmpty(filters)) {
            return;
        }
        List<String> admins = modelService.getModelAdmin(modelIds.get(0));
        if (!CollectionUtils.isEmpty(need2Apply)) {
            String promptInfo = String.format("当前结果已经过脱敏处理， 申请权限请联系管理员%s", admins);
            queryResultWithColumns.setQueryAuthorization(new QueryAuthorization(promptInfo));
        }
        if (!CollectionUtils.isEmpty(filters)) {
            log.debug("dimensionFilters:{}", filters);
            ModelResp modelResp = modelService.getModel(modelIds.get(0));
            List<String> exprList = new ArrayList<>();
            List<String> descList = new ArrayList<>();
            filters.stream().forEach(filter -> {
                descList.add(filter.getDescription());
                exprList.add(filter.getExpressions().toString());
            });
            String promptInfo = "当前结果已经过行权限过滤，详细过滤条件如下:%s, 申请权限请联系管理员%s";
            String message = String.format(promptInfo, CollectionUtils.isEmpty(descList) ? exprList : descList, admins);

            queryResultWithColumns.setQueryAuthorization(
                    new QueryAuthorization(modelResp.getName(), exprList, descList, message));
            log.info("queryResultWithColumns:{}", queryResultWithColumns);
        }
    }
}
