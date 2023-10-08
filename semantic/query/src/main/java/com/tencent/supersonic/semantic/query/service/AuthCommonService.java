package com.tencent.supersonic.semantic.query.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Sets;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

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

    public boolean doModelAdmin(User user, Long modelId) {
        List<ModelResp> modelListAdmin = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        if (CollectionUtils.isEmpty(modelListAdmin)) {
            return false;
        } else {
            Map<Long, List<ModelResp>> id2modelResp = modelListAdmin.stream()
                    .collect(Collectors.groupingBy(SchemaItem::getId));
            return !CollectionUtils.isEmpty(id2modelResp) && id2modelResp.containsKey(modelId);
        }
    }

    public void doModelVisible(User user, Long modelId) {
        Boolean visible = true;
        List<ModelResp> modelListVisible = modelService.getModelListWithAuth(user, null, AuthType.VISIBLE);
        if (CollectionUtils.isEmpty(modelListVisible)) {
            visible = false;
        } else {
            Map<Long, List<ModelResp>> id2domainDesc = modelListVisible.stream()
                    .collect(Collectors.groupingBy(SchemaItem::getId));
            if (!CollectionUtils.isEmpty(id2domainDesc) && !id2domainDesc.containsKey(modelId)) {
                visible = false;
            }
        }
        if (!visible) {
            ModelResp modelResp = modelService.getModel(modelId);
            String modelName = modelResp.getName();
            List<String> admins = modelService.getModelAdmin(modelResp.getId());
            String message = String.format("您没有主题域[%s]权限，请联系管理员%s开通", modelName, admins);
            throw new InvalidPermissionException(message);
        }

    }

    public Set<String> getHighSensitiveColsByModelId(Long modelId) {
        Set<String> highSensitiveCols = new HashSet<>();
        List<DimensionResp> highSensitiveDimensions = dimensionService.getHighSensitiveDimension(modelId);
        List<MetricResp> highSensitiveMetrics = metricService.getHighSensitiveMetric(modelId);
        if (!CollectionUtils.isEmpty(highSensitiveDimensions)) {
            highSensitiveDimensions.stream().forEach(dim -> highSensitiveCols.add(dim.getBizName()));
        }
        if (!CollectionUtils.isEmpty(highSensitiveMetrics)) {
            highSensitiveMetrics.stream().forEach(metric -> highSensitiveCols.add(metric.getBizName()));
        }
        return highSensitiveCols;
    }

    public AuthorizedResourceResp getAuthorizedResource(User user, Long domainId,
                                                           Set<String> sensitiveResReq) {
        List<AuthRes> resourceReqList = new ArrayList<>();
        sensitiveResReq.forEach(res -> resourceReqList.add(new AuthRes(domainId.toString(), res)));
        QueryAuthResReq queryAuthResReq = new QueryAuthResReq();
        queryAuthResReq.setResources(resourceReqList);
        queryAuthResReq.setModelId(domainId + "");
        AuthorizedResourceResp authorizedResource = fetchAuthRes(queryAuthResReq, user);
        log.info("user:{}, domainId:{}, after queryAuthorizedResources:{}", user.getName(), domainId,
                authorizedResource);
        return authorizedResource;
    }
    private AuthorizedResourceResp fetchAuthRes(QueryAuthResReq queryAuthResReq, User user) {
        log.info("queryAuthResReq:{}", queryAuthResReq);
        return authService.queryAuthorizedResources(queryAuthResReq, user);
    }
    public Set<String> getAuthResNameSet(AuthorizedResourceResp authorizedResource, Long domainId) {
        Set<String> resAuthName = new HashSet<>();
        List<AuthResGrp> authResGrpList = authorizedResource.getResources();
        authResGrpList.stream().forEach(authResGrp -> {
            List<AuthRes> cols = authResGrp.getGroup();
            if (!CollectionUtils.isEmpty(cols)) {
                cols.stream().filter(col -> domainId.equals(Long.parseLong(col.getModelId())))
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
                                                                  Long domainId, AuthorizedResourceResp authResource) {
        addPromptInfoInfo(domainId, resultWithColumns, authResource, Sets.newHashSet());
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

    public void addPromptInfoInfo(Long modelId, QueryResultWithSchemaResp queryResultWithColumns,
                                     AuthorizedResourceResp authorizedResource, Set<String> need2Apply) {
        List<DimensionFilter> filters = authorizedResource.getFilters();
        if (CollectionUtils.isEmpty(need2Apply) && CollectionUtils.isEmpty(filters)) {
            return;
        }
        List<String> admins = modelService.getModelAdmin(modelId);
        if (!CollectionUtils.isEmpty(need2Apply)) {
            String promptInfo = String.format("当前结果已经过脱敏处理， 申请权限请联系管理员%s", admins);
            queryResultWithColumns.setQueryAuthorization(new QueryAuthorization(promptInfo));
        }
        if (!CollectionUtils.isEmpty(filters)) {
            log.debug("dimensionFilters:{}", filters);
            ModelResp modelResp = modelService.getModel(modelId);
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
