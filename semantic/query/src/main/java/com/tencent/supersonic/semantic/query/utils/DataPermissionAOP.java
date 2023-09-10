package com.tencent.supersonic.semantic.query.utils;

import static com.tencent.supersonic.common.pojo.Constants.MINUS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthResGrp;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.pojo.QueryAuthorization;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.assertj.core.util.Sets;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Aspect
@Slf4j
public class DataPermissionAOP {

    private static final ObjectMapper MAPPER = new ObjectMapper().setDateFormat(
            new SimpleDateFormat(Constants.DAY_FORMAT));
    @Autowired
    private QueryStructUtils queryStructUtils;
    @Autowired
    private AuthService authService;
    @Autowired
    private DimensionService dimensionService;
    @Autowired
    private MetricService metricService;
    @Autowired
    private ModelService modelService;
    @Value("${permission.data.enable:true}")
    private Boolean permissionDataEnable;

    @Pointcut("@annotation(com.tencent.supersonic.semantic.query.service.DataPermission)")
    public void dataPermissionAOP() {
    }

    @Around(value = "dataPermissionAOP()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();
        QueryStructReq queryStructReq = (QueryStructReq) args[0];
        User user = (User) args[1];

        if (!permissionDataEnable) {
            log.info("permissionDataEnable is false");
            return point.proceed();
        }

        if (Objects.isNull(user) || Strings.isNullOrEmpty(user.getName())) {
            throw new RuntimeException("lease provide user information");
        }
        //1. determine whether admin of the model
        if (doModelAdmin(user, queryStructReq)) {
            return point.proceed();
        }

        // 2. determine whether the subject field is visible
        doModelVisible(user, queryStructReq);

        // 3. fetch data permission meta information
        Long modelId = queryStructReq.getModelId();
        Set<String> res4Privilege = queryStructUtils.getResNameEnExceptInternalCol(queryStructReq);
        log.info("modelId:{}, res4Privilege:{}", modelId, res4Privilege);

        Set<String> sensitiveResByModel = getHighSensitiveColsByModelId(modelId);
        Set<String> sensitiveResReq = res4Privilege.parallelStream()
                .filter(sensitiveResByModel::contains).collect(Collectors.toSet());
        log.info("this query domainId:{}, sensitiveResReq:{}", modelId, sensitiveResReq);

        // query user privilege info
        AuthorizedResourceResp authorizedResource = getAuthorizedResource(user, modelId, sensitiveResReq);
        // get sensitiveRes that user has privilege
        Set<String> resAuthSet = getAuthResNameSet(authorizedResource, queryStructReq.getModelId());

        // 4.if sensitive fields without permission are involved in filter, thrown an exception
        doFilterCheckLogic(queryStructReq, resAuthSet, sensitiveResReq);

        // 5.row permission pre-filter
        doRowPermission(queryStructReq, authorizedResource);

        // 6.proceed
        QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) point.proceed();

        if (CollectionUtils.isEmpty(sensitiveResReq) || allSensitiveResReqIsOk(sensitiveResReq, resAuthSet)) {
            // if sensitiveRes is empty
            log.info("sensitiveResReq is empty");
            return getQueryResultWithColumns(queryResultWithColumns, modelId, authorizedResource);
        }

        // 6.if the column has no permission, hit *
        Set<String> need2Apply = sensitiveResReq.stream().filter(req -> !resAuthSet.contains(req))
                .collect(Collectors.toSet());
        QueryResultWithSchemaResp queryResultAfterDesensitization = desensitizationData(queryResultWithColumns,
                need2Apply);
        addPromptInfoInfo(modelId, queryResultAfterDesensitization, authorizedResource, need2Apply);

        return queryResultAfterDesensitization;

    }

    private boolean doModelAdmin(User user, QueryStructReq queryStructReq) {
        Long modelId = queryStructReq.getModelId();
        List<ModelResp> modelListAdmin = modelService.getModelListWithAuth(user.getName(), null, AuthType.ADMIN);
        if (CollectionUtils.isEmpty(modelListAdmin)) {
            return false;
        } else {
            Map<Long, List<ModelResp>> id2modelResp = modelListAdmin.stream()
                    .collect(Collectors.groupingBy(SchemaItem::getId));
            return !CollectionUtils.isEmpty(id2modelResp) && id2modelResp.containsKey(modelId);
        }
    }

    private void doModelVisible(User user, QueryStructReq queryStructReq) {
        Boolean visible = true;
        Long modelId = queryStructReq.getModelId();
        List<ModelResp> modelListVisible = modelService.getModelListWithAuth(user.getName(), null, AuthType.VISIBLE);
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

    private QueryResultWithSchemaResp getQueryResultWithColumns(QueryResultWithSchemaResp resultWithColumns,
            Long domainId, AuthorizedResourceResp authResource) {
        addPromptInfoInfo(domainId, resultWithColumns, authResource, Sets.newHashSet());
        return resultWithColumns;
    }

    private void addPromptInfoInfo(Long modelId, QueryResultWithSchemaResp queryResultWithColumns,
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

    private boolean allSensitiveResReqIsOk(Set<String> sensitiveResReq, Set<String> resAuthSet) {
        if (resAuthSet.containsAll(sensitiveResReq)) {
            return true;
        }
        log.info("sensitiveResReq:{}, resAuthSet:{}", sensitiveResReq, resAuthSet);
        return false;
    }

    private Set<String> getAuthResNameSet(AuthorizedResourceResp authorizedResource, Long domainId) {
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

    private AuthorizedResourceResp getAuthorizedResource(User user, Long domainId,
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

    private Set<String> getHighSensitiveColsByModelId(Long modelId) {
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

    private void doRowPermission(QueryStructReq queryStructReq, AuthorizedResourceResp authorizedResource) {
        log.debug("start doRowPermission logic");
        StringJoiner joiner = new StringJoiner(" OR ");
        List<String> dimensionFilters = new ArrayList<>();
        if (!CollectionUtils.isEmpty(authorizedResource.getFilters())) {
            authorizedResource.getFilters().stream()
                    .forEach(filter -> dimensionFilters.addAll(filter.getExpressions()));
        }

        if (CollectionUtils.isEmpty(dimensionFilters)) {
            log.debug("dimensionFilters is empty");
            return;
        }

        dimensionFilters.stream().forEach(filter -> {
            if (StringUtils.isNotEmpty(filter) && StringUtils.isNotEmpty(filter.trim())) {
                joiner.add(" ( " + filter + " ) ");
            }
        });

        if (StringUtils.isNotEmpty(joiner.toString())) {
            log.info("before doRowPermission, queryStructReq:{}", queryStructReq);
            Filter filter = new Filter("", FilterOperatorEnum.SQL_PART, joiner.toString());
            List<Filter> filters = Objects.isNull(queryStructReq.getOriginalFilter()) ? new ArrayList<>()
                    : queryStructReq.getOriginalFilter();
            filters.add(filter);
            queryStructReq.setDimensionFilters(filters);
            log.info("after doRowPermission, queryStructReq:{}", queryStructReq);
        }

    }

    private QueryResultWithSchemaResp desensitizationData(QueryResultWithSchemaResp raw, Set<String> need2Apply) {
        log.debug("start desensitizationData logic");
        if (CollectionUtils.isEmpty(need2Apply)) {
            log.info("user has all sensitiveRes");
            return raw;
        }

        List<QueryColumn> columns = raw.getColumns();

        boolean doDesensitization = false;
        for (QueryColumn queryColumn : columns) {
            if (need2Apply.contains(queryColumn.getNameEn())) {
                doDesensitization = true;
                break;
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
                if (need2Apply.contains(col)) {
                    newRow.put(col, "****");
                } else {
                    newRow.put(col, row.get(col));
                }
            }
            result.set(i, newRow);
        }
    }

    private void doFilterCheckLogic(QueryStructReq queryStructReq, Set<String> resAuthName,
            Set<String> sensitiveResReq) {
        Set<String> resFilterSet = queryStructUtils.getFilterResNameEnExceptInternalCol(queryStructReq);
        Set<String> need2Apply = resFilterSet.stream()
                .filter(res -> !resAuthName.contains(res) && sensitiveResReq.contains(res)).collect(Collectors.toSet());
        Set<String> nameCnSet = new HashSet<>();

        List<Long> modelIds = new ArrayList<>();
        modelIds.add(queryStructReq.getModelId());
        List<ModelResp> modelInfos = modelService.getModelList(modelIds);
        String modelNameCn = Constants.EMPTY;
        if (!CollectionUtils.isEmpty(modelInfos)) {
            modelNameCn = modelInfos.get(0).getName();
        }

        List<DimensionResp> dimensionDescList = dimensionService.getDimensions(queryStructReq.getModelId());
        String finalDomainNameCn = modelNameCn;
        dimensionDescList.stream().filter(dim -> need2Apply.contains(dim.getBizName()))
                .forEach(dim -> nameCnSet.add(finalDomainNameCn + MINUS + dim.getName()));

        if (!CollectionUtils.isEmpty(need2Apply)) {
            ModelResp modelResp = modelInfos.get(0);
            List<String> admins = modelService.getModelAdmin(modelResp.getId());
            log.info("in doFilterLogic, need2Apply:{}", need2Apply);
            String message = String.format("您没有以下维度%s权限, 请联系管理员%s开通", nameCnSet, admins);
            throw new InvalidPermissionException(message);
        }
    }


    private AuthorizedResourceResp fetchAuthRes(QueryAuthResReq queryAuthResReq, User user) {
        log.info("queryAuthResReq:{}", queryAuthResReq);
        return authService.queryAuthorizedResources(queryAuthResReq, user);
    }

}
