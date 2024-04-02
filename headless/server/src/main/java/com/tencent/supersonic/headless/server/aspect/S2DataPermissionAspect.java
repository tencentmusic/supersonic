package com.tencent.supersonic.headless.server.aspect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthResGrp;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.QueryAuthorization;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.utils.QueryStructUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static com.tencent.supersonic.common.pojo.Constants.MINUS;

@Component
@Aspect
@Order(1)
@Slf4j
public class S2DataPermissionAspect {

    private static final ObjectMapper MAPPER = new ObjectMapper().setDateFormat(
            new SimpleDateFormat(Constants.DAY_FORMAT));

    @Autowired
    private QueryStructUtils queryStructUtils;
    @Autowired
    private DimensionService dimensionService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private SchemaService schemaService;
    @Autowired
    private DataSetService dataSetService;
    @Autowired
    private AuthService authService;

    @Pointcut("@annotation(com.tencent.supersonic.headless.server.annotation.S2DataPermission)")
    private void s2PermissionCheck() {
    }

    @Around("s2PermissionCheck()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] objects = joinPoint.getArgs();
        SemanticQueryReq queryReq = (SemanticQueryReq) objects[0];
        if (!queryReq.isNeedAuth()) {
            log.info("needAuth is false, there is no need to check permissions.");
            return joinPoint.proceed();
        }
        User user = (User) objects[1];
        if (Objects.isNull(user) || Strings.isNullOrEmpty(user.getName())) {
            throw new RuntimeException("please provide user information");
        }
        List<Long> modelIds = getModelsInDataSet(queryReq);

        // determine whether admin of the model
        if (doModelAdmin(user, modelIds)) {
            return joinPoint.proceed();
        }
        // determine whether the subject field is visible
        doModelVisible(user, modelIds);

        if (queryReq instanceof QuerySqlReq) {
            return checkSqlPermission(joinPoint, (QuerySqlReq) queryReq);
        }
        if (queryReq instanceof QueryStructReq) {
            return checkStructPermission(joinPoint, (QueryStructReq) queryReq);
        }
        throw new InvalidArgumentException("queryReq is not Invalid:" + queryReq);
    }

    private Object checkSqlPermission(ProceedingJoinPoint joinPoint, QuerySqlReq querySqlReq)
            throws Throwable {
        Object[] objects = joinPoint.getArgs();
        User user = (User) objects[1];
        // fetch data permission meta information
        SchemaFilterReq filter = new SchemaFilterReq();
        filter.setModelIds(querySqlReq.getModelIds());
        filter.setDataSetId(querySqlReq.getDataSetId());
        SemanticSchemaResp semanticSchemaResp = schemaService.fetchSemanticSchema(filter);
        List<Long> modelIdInDataSet = semanticSchemaResp.getModelResps().stream()
                .map(ModelResp::getId).collect(Collectors.toList());
        Set<String> res4Privilege = queryStructUtils.getResNameEnExceptInternalCol(querySqlReq, semanticSchemaResp);
        log.info("modelId:{}, res4Privilege:{}", modelIdInDataSet, res4Privilege);

        Set<String> sensitiveResByModel = getHighSensitiveColsByModelId(semanticSchemaResp);
        Set<String> sensitiveResReq = res4Privilege.parallelStream()
                .filter(sensitiveResByModel::contains).collect(Collectors.toSet());

        // query user privilege info
        AuthorizedResourceResp authorizedResource = getAuthorizedResource(user, modelIdInDataSet, sensitiveResReq);
        // get sensitiveRes that user has privilege
        Set<String> resAuthSet = getAuthResNameSet(authorizedResource, modelIdInDataSet);

        // if sensitive fields without permission are involved in filter, thrown an exception
        doFilterCheckLogic(querySqlReq, resAuthSet, sensitiveResReq);

        // row permission pre-filter
        doRowPermission(querySqlReq, authorizedResource);

        // proceed
        SemanticQueryResp queryResultWithColumns = (SemanticQueryResp) joinPoint.proceed();

        if (CollectionUtils.isEmpty(sensitiveResReq) || allSensitiveResReqIsOk(sensitiveResReq, resAuthSet)) {
            // if sensitiveRes is empty
            log.info("sensitiveResReq is empty");
            return getQueryResultWithColumns(queryResultWithColumns, modelIdInDataSet, authorizedResource);
        }

        // if the column has no permission, hit *
        Set<String> need2Apply = sensitiveResReq.stream().filter(req -> !resAuthSet.contains(req))
                .collect(Collectors.toSet());
        log.info("need2Apply:{},sensitiveResReq:{},resAuthSet:{}", need2Apply, sensitiveResReq, resAuthSet);
        SemanticQueryResp queryResultAfterDesensitization =
                desensitizationData(queryResultWithColumns, need2Apply);
        addPromptInfoInfo(modelIdInDataSet, queryResultAfterDesensitization, authorizedResource, need2Apply);

        return queryResultAfterDesensitization;
    }

    private void doFilterCheckLogic(QuerySqlReq querySqlReq, Set<String> resAuthName,
            Set<String> sensitiveResReq) {
        Set<String> resFilterSet = queryStructUtils.getFilterResNameEnExceptInternalCol(querySqlReq);
        Set<String> need2Apply = resFilterSet.stream()
                .filter(res -> !resAuthName.contains(res) && sensitiveResReq.contains(res)).collect(Collectors.toSet());
        Set<String> nameCnSet = new HashSet<>();

        List<Long> modelIds = Lists.newArrayList(querySqlReq.getModelIds());
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setModelIds(modelIds);
        List<ModelResp> modelInfos = modelService.getModelList(modelFilter);
        String modelNameCn = Constants.EMPTY;
        if (!CollectionUtils.isEmpty(modelInfos)) {
            modelNameCn = modelInfos.get(0).getName();
        }
        MetaFilter metaFilter = new MetaFilter(modelIds);
        List<DimensionResp> dimensionDescList = dimensionService.getDimensions(metaFilter);
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

    private void doFilterCheckLogic(Set<String> resAuthName, Set<String> sensitiveResReq,
                                    List<Long> modelIdInDataSet, QueryStructReq queryStructReq) {
        Set<String> resFilterSet = queryStructUtils.getFilterResNameEnExceptInternalCol(queryStructReq);
        Set<String> need2Apply = resFilterSet.stream()
                .filter(res -> !resAuthName.contains(res) && sensitiveResReq.contains(res)).collect(Collectors.toSet());
        Set<String> nameCnSet = new HashSet<>();
        ModelFilter modelFilter = new ModelFilter(false, modelIdInDataSet);
        Map<Long, ModelResp> modelRespMap = modelService.getModelMap(modelFilter);
        List<DimensionResp> dimensionDescList = dimensionService.getDimensions(new MetaFilter(modelIdInDataSet));
        dimensionDescList.stream().filter(dim -> need2Apply.contains(dim.getBizName()))
                .forEach(dim -> nameCnSet.add(modelRespMap.get(dim.getModelId()).getName() + MINUS + dim.getName()));

        if (!CollectionUtils.isEmpty(need2Apply)) {
            List<String> admins = modelService.getModelAdmin(modelIdInDataSet.get(0));
            log.info("in doFilterLogic, need2Apply:{}", need2Apply);
            String message = String.format("您没有以下维度%s权限, 请联系管理员%s开通", nameCnSet, admins);
            throw new InvalidPermissionException(message);
        }
    }

    public Object checkStructPermission(ProceedingJoinPoint point, QueryStructReq queryStructReq) throws Throwable {
        Object[] args = point.getArgs();
        User user = (User) args[1];
        // fetch data permission meta information
        SchemaFilterReq filter = new SchemaFilterReq();
        filter.setModelIds(queryStructReq.getModelIds());
        filter.setDataSetId(queryStructReq.getDataSetId());
        SemanticSchemaResp semanticSchemaResp = schemaService.fetchSemanticSchema(filter);
        List<Long> modelIdInDataSet = semanticSchemaResp.getModelResps().stream()
                .map(ModelResp::getId).collect(Collectors.toList());
        Set<String> res4Privilege = queryStructUtils.getResNameEnExceptInternalCol(queryStructReq);
        log.info("modelId:{}, res4Privilege:{}", modelIdInDataSet, res4Privilege);

        Set<String> sensitiveResByModel = getHighSensitiveColsByModelId(semanticSchemaResp);
        Set<String> sensitiveResReq = res4Privilege.parallelStream()
                .filter(sensitiveResByModel::contains).collect(Collectors.toSet());
        log.info("this query domainId:{}, sensitiveResReq:{}", modelIdInDataSet, sensitiveResReq);

        // query user privilege info
        AuthorizedResourceResp authorizedResource = getAuthorizedResource(user,
                modelIdInDataSet, sensitiveResReq);
        // get sensitiveRes that user has privilege
        Set<String> resAuthSet = getAuthResNameSet(authorizedResource, modelIdInDataSet);

        // if sensitive fields without permission are involved in filter, thrown an exception
        doFilterCheckLogic(resAuthSet, sensitiveResReq, modelIdInDataSet, queryStructReq);

        // row permission pre-filter
        doRowPermission(queryStructReq, authorizedResource);

        // proceed
        SemanticQueryResp queryResultWithColumns = (SemanticQueryResp) point.proceed();

        if (CollectionUtils.isEmpty(sensitiveResReq) || allSensitiveResReqIsOk(sensitiveResReq, resAuthSet)) {
            // if sensitiveRes is empty
            log.info("sensitiveResReq is empty");
            return getQueryResultWithColumns(queryResultWithColumns, modelIdInDataSet, authorizedResource);
        }

        // if the column has no permission, hit *
        Set<String> need2Apply = sensitiveResReq.stream().filter(req -> !resAuthSet.contains(req))
                .collect(Collectors.toSet());
        SemanticQueryResp queryResultAfterDesensitization =
                desensitizationData(queryResultWithColumns, need2Apply);
        addPromptInfoInfo(modelIdInDataSet, queryResultAfterDesensitization, authorizedResource, need2Apply);

        return queryResultAfterDesensitization;

    }

    public boolean allSensitiveResReqIsOk(Set<String> sensitiveResReq, Set<String> resAuthSet) {
        if (resAuthSet.containsAll(sensitiveResReq)) {
            return true;
        }
        log.info("sensitiveResReq:{}, resAuthSet:{}", sensitiveResReq, resAuthSet);
        return false;
    }

    private void doRowPermission(QuerySqlReq querySqlReq, AuthorizedResourceResp authorizedResource) {
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
        try {
            Expression expression = CCJSqlParserUtil.parseCondExpression(" ( " + joiner + " ) ");
            if (StringUtils.isNotEmpty(joiner.toString())) {
                String sql = SqlAddHelper.addWhere(querySqlReq.getSql(), expression);
                log.info("before doRowPermission, queryS2SQLReq:{}", querySqlReq.getSql());
                querySqlReq.setSql(sql);
                log.info("after doRowPermission, queryS2SQLReq:{}", querySqlReq.getSql());
            }
        } catch (JSQLParserException jsqlParserException) {
            log.info("jsqlParser has an exception:{}", jsqlParserException.toString());
        }

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
        List<Long> modelListVisible = modelService.getModelListWithAuth(user, null, AuthType.VISIBLE)
                .stream().map(ModelResp::getId).collect(Collectors.toList());
        modelIds.removeAll(modelListVisible);
        if (!CollectionUtils.isEmpty(modelIds)) {
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setIds(modelIds);
            List<ModelResp> modelResps = modelService.getModelList(metaFilter);
            ModelResp modelResp = modelResps.stream().findFirst().orElse(null);
            if (modelResp == null) {
                throw new InvalidArgumentException("查询的模型不存在");
            }
            String message = String.format("您没有模型[%s]权限，请联系管理员%s开通", modelResp.getName(), modelResp.getAdmins());
            throw new InvalidPermissionException(message);
        }
    }

    public Set<String> getHighSensitiveColsByModelId(SemanticSchemaResp semanticSchemaResp) {
        Set<String> highSensitiveCols = new HashSet<>();
        if (!CollectionUtils.isEmpty(semanticSchemaResp.getDimensions())) {
            semanticSchemaResp.getDimensions().stream().filter(dimSchemaResp ->
                            SensitiveLevelEnum.HIGH.getCode().equals(dimSchemaResp.getSensitiveLevel()))
                    .forEach(dim -> highSensitiveCols.add(dim.getBizName()));
        }
        if (!CollectionUtils.isEmpty(semanticSchemaResp.getMetrics())) {
            semanticSchemaResp.getMetrics().stream().filter(metricSchemaResp ->
                            SensitiveLevelEnum.HIGH.getCode().equals(metricSchemaResp.getSensitiveLevel()))
                    .forEach(metric -> highSensitiveCols.add(metric.getBizName()));
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

    public SemanticQueryResp getQueryResultWithColumns(SemanticQueryResp resultWithColumns,
                                                       List<Long> modelIds,
                                                       AuthorizedResourceResp authResource) {
        addPromptInfoInfo(modelIds, resultWithColumns, authResource, Sets.newHashSet());
        return resultWithColumns;
    }

    public SemanticQueryResp desensitizationData(SemanticQueryResp raw, Set<String> need2Apply) {
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

        SemanticQueryResp queryResultWithColumns = raw;
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
            if (need2Apply.contains(getName(col.getNameEn()))) {
                col.setAuthorized(false);
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

    private SemanticQueryResp deepCopyResult(SemanticQueryResp raw) throws Exception {
        SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();
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

    public void addPromptInfoInfo(List<Long> modelIds, SemanticQueryResp queryResultWithColumns,
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

    private List<Long> getModelsInDataSet(SemanticQueryReq queryReq) {
        List<Long> modelIds = queryReq.getModelIds();
        if (queryReq.getDataSetId() != null) {
            DataSetResp dataSetResp = dataSetService.getDataSet(queryReq.getDataSetId());
            modelIds = dataSetResp.getAllModels();
        }
        return modelIds;
    }

}
