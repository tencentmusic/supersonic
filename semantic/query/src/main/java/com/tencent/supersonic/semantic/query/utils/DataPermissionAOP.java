package com.tencent.supersonic.semantic.query.utils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import com.tencent.supersonic.semantic.query.service.AuthCommonService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.MINUS;

@Component
@Aspect
@Slf4j
public class DataPermissionAOP {
    @Autowired
    private QueryStructUtils queryStructUtils;
    @Autowired
    private DimensionService dimensionService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private AuthCommonService authCommonService;
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
        if (authCommonService.doModelAdmin(user, queryStructReq.getModelIds())) {
            return point.proceed();
        }

        // 2. determine whether the subject field is visible
        authCommonService.doModelVisible(user, queryStructReq.getModelIds());

        // 3. fetch data permission meta information
        List<Long> modelIds = queryStructReq.getModelIds();
        Set<String> res4Privilege = queryStructUtils.getResNameEnExceptInternalCol(queryStructReq);
        log.info("modelId:{}, res4Privilege:{}", modelIds, res4Privilege);

        Set<String> sensitiveResByModel = authCommonService.getHighSensitiveColsByModelId(modelIds);
        Set<String> sensitiveResReq = res4Privilege.parallelStream()
                .filter(sensitiveResByModel::contains).collect(Collectors.toSet());
        log.info("this query domainId:{}, sensitiveResReq:{}", modelIds, sensitiveResReq);

        // query user privilege info
        AuthorizedResourceResp authorizedResource = authCommonService.getAuthorizedResource(user,
                modelIds, sensitiveResReq);
        // get sensitiveRes that user has privilege
        Set<String> resAuthSet = authCommonService.getAuthResNameSet(authorizedResource,
                queryStructReq.getModelIds());

        // 4.if sensitive fields without permission are involved in filter, thrown an exception
        doFilterCheckLogic(queryStructReq, resAuthSet, sensitiveResReq);

        // 5.row permission pre-filter
        doRowPermission(queryStructReq, authorizedResource);

        // 6.proceed
        QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) point.proceed();

        if (CollectionUtils.isEmpty(sensitiveResReq) || allSensitiveResReqIsOk(sensitiveResReq, resAuthSet)) {
            // if sensitiveRes is empty
            log.info("sensitiveResReq is empty");
            return authCommonService.getQueryResultWithColumns(queryResultWithColumns, modelIds, authorizedResource);
        }

        // 6.if the column has no permission, hit *
        Set<String> need2Apply = sensitiveResReq.stream().filter(req -> !resAuthSet.contains(req))
                .collect(Collectors.toSet());
        QueryResultWithSchemaResp queryResultAfterDesensitization =
                authCommonService.desensitizationData(queryResultWithColumns, need2Apply);
        authCommonService.addPromptInfoInfo(modelIds, queryResultAfterDesensitization, authorizedResource, need2Apply);

        return queryResultAfterDesensitization;

    }

    private boolean allSensitiveResReqIsOk(Set<String> sensitiveResReq, Set<String> resAuthSet) {
        if (resAuthSet.containsAll(sensitiveResReq)) {
            return true;
        }
        log.info("sensitiveResReq:{}, resAuthSet:{}", sensitiveResReq, resAuthSet);
        return false;
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

    private void doFilterCheckLogic(QueryStructReq queryStructReq, Set<String> resAuthName,
            Set<String> sensitiveResReq) {
        Set<String> resFilterSet = queryStructUtils.getFilterResNameEnExceptInternalCol(queryStructReq);
        Set<String> need2Apply = resFilterSet.stream()
                .filter(res -> !resAuthName.contains(res) && sensitiveResReq.contains(res)).collect(Collectors.toSet());
        Set<String> nameCnSet = new HashSet<>();

        Map<Long, ModelResp> modelRespMap = modelService.getModelMap();
        List<Long> modelIds = Lists.newArrayList(queryStructReq.getModelIds());
        List<DimensionResp> dimensionDescList = dimensionService.getDimensions(new MetaFilter(modelIds));
        dimensionDescList.stream().filter(dim -> need2Apply.contains(dim.getBizName()))
                .forEach(dim -> nameCnSet.add(modelRespMap.get(dim.getModelId()).getName() + MINUS + dim.getName()));

        if (!CollectionUtils.isEmpty(need2Apply)) {
            List<String> admins = modelService.getModelAdmin(modelIds.get(0));
            log.info("in doFilterLogic, need2Apply:{}", need2Apply);
            String message = String.format("您没有以下维度%s权限, 请联系管理员%s开通", nameCnSet, admins);
            throw new InvalidPermissionException(message);
        }
    }

}
