package com.tencent.supersonic.semantic.query.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.MINUS;

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
import com.tencent.supersonic.semantic.api.core.pojo.QueryAuthorization;
import com.tencent.supersonic.semantic.api.core.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.DomainResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.exception.InvalidArgumentException;
import com.tencent.supersonic.common.exception.InvalidPermissionException;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.core.domain.MetricService;

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
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Aspect
@Slf4j
public class DataPermissionAOP {

    @Autowired
    private QueryStructUtils queryStructUtils;

    @Autowired
    private AuthService authService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private MetricService metricService;

    @Autowired
    private DomainService domainService;

    @Value("${permission.data.enable:true}")
    private Boolean permissionDataEnable;

    private static final ObjectMapper MAPPER = new ObjectMapper().setDateFormat(
            new SimpleDateFormat(Constants.DAY_FORMAT));

    @Pointcut("@annotation(com.tencent.supersonic.semantic.query.domain.annotation.DataPermission)")
    public void dataPermissionAOP() {
    }

    @Around(value = "dataPermissionAOP()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();
        QueryStructReq queryStructCmd = (QueryStructReq) args[0];
        User user = (User) args[1];

        if (!permissionDataEnable) {
            log.info("permissionDataEnable is false");
            return point.proceed();
        }

        if (Objects.isNull(user) || Strings.isNullOrEmpty(user.getName())) {
            throw new RuntimeException("lease provide user information");
        }

        // 1. determine whether the subject field is visible
        doDomainVisible(user, queryStructCmd);

        // 2. fetch data permission meta information
        Long domainId = queryStructCmd.getDomainId();
        Set<String> res4Privilege = queryStructUtils.getResNameEnExceptInternalCol(queryStructCmd);
        log.info("classId:{}, res4Privilege:{}", domainId, res4Privilege);

        Set<String> sensitiveResByDomain = getHighSensitiveColsByDomainId(domainId);
        Set<String> sensitiveResReq = res4Privilege.parallelStream()
                .filter(res -> sensitiveResByDomain.contains(res)).collect(Collectors.toSet());
        log.info("this query domainId:{}, sensitiveResReq:{}", domainId, sensitiveResReq);

        // query user privilege info
        HttpServletRequest request = (HttpServletRequest) args[2];
        AuthorizedResourceResp authorizedResource = getAuthorizedResource(user, request, domainId, sensitiveResReq);
        // get sensitiveRes that user has privilege
        Set<String> resAuthSet = getAuthResNameSet(authorizedResource, queryStructCmd.getDomainId());

        // 3.if sensitive fields without permission are involved in filter, thrown an exception
        doFilterCheckLogic(queryStructCmd, resAuthSet, sensitiveResReq);

        // 4.row permission pre-filter
        doRowPermission(queryStructCmd, authorizedResource);

        // 5.proceed
        QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) point.proceed();

        if (CollectionUtils.isEmpty(sensitiveResReq) || allSensitiveResReqIsOk(sensitiveResReq, resAuthSet)) {
            // if sensitiveRes is empty
            log.info("sensitiveResReq is empty");
            return getQueryResultWithColumns(queryResultWithColumns, domainId, authorizedResource);
        }

        // 6.if the column has no permission, hit *
        Set<String> need2Apply = sensitiveResReq.stream().filter(req -> !resAuthSet.contains(req))
                .collect(Collectors.toSet());
        QueryResultWithSchemaResp queryResultAfterDesensitization = desensitizationData(queryResultWithColumns,
                need2Apply);
        addPromptInfoInfo(domainId, queryResultAfterDesensitization, authorizedResource);

        return queryResultAfterDesensitization;

    }

    private void doDomainVisible(User user, QueryStructReq queryStructCmd) {
        Boolean visible = true;
        Long domainId = queryStructCmd.getDomainId();
        List<DomainResp> classListForViewer = domainService.getDomainListForViewer(user.getName());
        if (CollectionUtils.isEmpty(classListForViewer)) {
            visible = false;
        } else {
            Map<Long, List<DomainResp>> id2domainDesc = classListForViewer.stream()
                    .collect(Collectors.groupingBy(classInfo -> classInfo.getId()));
            if (!CollectionUtils.isEmpty(id2domainDesc) && !id2domainDesc.containsKey(domainId)) {
                visible = false;
            }
        }

        if (!visible) {
            List<Long> domainIds = new ArrayList<>();
            domainIds.add(domainId);
            List<DomainResp> classInfos = domainService.getDomainList(domainIds);
            if (CollectionUtils.isEmpty(classInfos)) {
                throw new InvalidArgumentException(
                        "invalid domainId:" + domainId + ", please contact admin for details");
            }
            String domainName = classInfos.get(0).getName();
            throw new InvalidPermissionException(
                    "You do not have domain:" + domainName + " permission, please contact admin for details");

        }

    }

    private QueryResultWithSchemaResp getQueryResultWithColumns(QueryResultWithSchemaResp resultWithColumns,
            Long domainId, AuthorizedResourceResp authResource) {
        addPromptInfoInfo(domainId, resultWithColumns, authResource);
        return resultWithColumns;
    }

    private void addPromptInfoInfo(Long domainId, QueryResultWithSchemaResp queryResultWithColumns,
                                   AuthorizedResourceResp authorizedResource) {
        List<DimensionFilter> filters = authorizedResource.getFilters();
        if (!CollectionUtils.isEmpty(filters)) {
            log.debug("dimensionFilters:{}", filters);

            List<Long> domainIds = new ArrayList<>();
            domainIds.add(domainId);
            List<DomainResp> classInfos = domainService.getDomainList(domainIds);
            String classNameCn = "";
            if (!CollectionUtils.isEmpty(classInfos)) {
                classNameCn = classInfos.get(0).getName();
            }

            List<String> exprList = new ArrayList<>();
            List<String> descList = new ArrayList<>();
            filters.stream().forEach(filter -> {
                descList.add(filter.getDescription());
                exprList.add(filter.getExpressions().toString());
            });

            String promptInfo = "the current data has been controlled by permissions,"
                    + " related information:%s, please contact admin for details";
            String message = String.format(promptInfo, CollectionUtils.isEmpty(descList) ? exprList : descList);

            queryResultWithColumns.setQueryAuthorization(
                    new QueryAuthorization(classNameCn, exprList, descList, message));
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
                cols.stream().filter(col -> domainId.equals(Long.parseLong(col.getDomainId())))
                        .forEach(col -> resAuthName.add(col.getName()));
            }

        });
        log.info("resAuthName:{}", resAuthName);
        return resAuthName;
    }

    private AuthorizedResourceResp getAuthorizedResource(User user, HttpServletRequest request, Long domainId,
                                                         Set<String> sensitiveResReq) {
        List<AuthRes> resourceReqList = new ArrayList<>();
        sensitiveResReq.stream().forEach(res -> resourceReqList.add(new AuthRes(domainId.toString(), res)));
        QueryAuthResReq queryAuthResReq = new QueryAuthResReq();
        queryAuthResReq.setUser(user.getName());
        queryAuthResReq.setResources(resourceReqList);
        queryAuthResReq.setDomainId(domainId + "");
        AuthorizedResourceResp authorizedResource = fetchAuthRes(request, queryAuthResReq);
        log.info("user:{}, domainId:{}, after queryAuthorizedResources:{}", user.getName(), domainId,
                authorizedResource);
        return authorizedResource;
    }

    private Set<String> getHighSensitiveColsByDomainId(Long domainId) {
        Set<String> highSensitiveCols = new HashSet<>();
        List<DimensionResp> highSensitiveDimensions = dimensionService.getHighSensitiveDimension(domainId);
        List<MetricResp> highSensitiveMetrics = metricService.getHighSensitiveMetric(domainId);
        if (!CollectionUtils.isEmpty(highSensitiveDimensions)) {
            highSensitiveDimensions.stream().forEach(dim -> highSensitiveCols.add(dim.getBizName()));
        }
        if (!CollectionUtils.isEmpty(highSensitiveMetrics)) {
            highSensitiveMetrics.stream().forEach(metric -> highSensitiveCols.add(metric.getBizName()));
        }
        return highSensitiveCols;
    }

    private void doRowPermission(QueryStructReq queryStructCmd, AuthorizedResourceResp authorizedResource) {
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
            log.info("before doRowPermission, queryStructCmd:{}", queryStructCmd);
            Filter filter = new Filter("", FilterOperatorEnum.SQL_PART, joiner.toString());
            List<Filter> filters = Objects.isNull(queryStructCmd.getOriginalFilter()) ? new ArrayList<>()
                    : queryStructCmd.getOriginalFilter();
            filters.add(filter);
            queryStructCmd.setDimensionFilters(filters);
            log.info("after doRowPermission, queryStructCmd:{}", queryStructCmd);
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
            log.warn("deepCopyResult, e:{}", e);
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

    private void doFilterCheckLogic(QueryStructReq queryStructCmd, Set<String> resAuthName,
                                    Set<String> sensitiveResReq) {
        Set<String> resFilterSet = queryStructUtils.getFilterResNameEnExceptInternalCol(queryStructCmd);
        Set<String> need2Apply = resFilterSet.stream()
                .filter(res -> !resAuthName.contains(res) && sensitiveResReq.contains(res)).collect(Collectors.toSet());
        Set<String> nameCnSet = new HashSet<>();

        List<Long> domainIds = new ArrayList<>();
        domainIds.add(queryStructCmd.getDomainId());
        List<DomainResp> classInfos = domainService.getDomainList(domainIds);
        String classNameCn = Constants.EMPTY;
        if (!CollectionUtils.isEmpty(classInfos)) {
            classNameCn = classInfos.get(0).getName();
        }

        List<DimensionResp> dimensionDescList = dimensionService.getDimensions(queryStructCmd.getDomainId());
        String finalDomainNameCn = classNameCn;
        dimensionDescList.stream().filter(dim -> need2Apply.contains(dim.getBizName()))
                .forEach(dim -> nameCnSet.add(finalDomainNameCn + MINUS + dim.getName()));

        if (!CollectionUtils.isEmpty(need2Apply)) {
            log.warn("in doFilterLogic, need2Apply:{}", need2Apply);
            throw new InvalidPermissionException(
                    "you do not have data permission:" + nameCnSet + ", please contact admin for details");
        }
    }


    private AuthorizedResourceResp fetchAuthRes(HttpServletRequest request, QueryAuthResReq queryAuthResReq) {
        log.info("Authorization:{}", request.getHeader("Authorization"));
        log.info("queryAuthResReq:{}", queryAuthResReq);
        return authService.queryAuthorizedResources(request, queryAuthResReq);
    }

}