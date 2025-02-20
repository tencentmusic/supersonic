package com.tencent.supersonic.headless.server.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.Dimension;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.DateInfoReq;
import com.tencent.supersonic.headless.api.pojo.request.DimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.FieldRemovedReq;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.UnAvailableItemResp;
import com.tencent.supersonic.headless.server.modeller.SemanticModeller;
import com.tencent.supersonic.headless.server.persistence.dataobject.DateInfoDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelDO;
import com.tencent.supersonic.headless.server.persistence.repository.DateInfoRepository;
import com.tencent.supersonic.headless.server.persistence.repository.ModelRepository;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.utils.CoreComponentFactory;
import com.tencent.supersonic.headless.server.utils.ModelConverter;
import com.tencent.supersonic.headless.server.utils.NameCheckUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;

    private final DatabaseService databaseService;

    private final DimensionService dimensionService;

    private final MetricService metricService;

    private final DomainService domainService;

    private final UserService userService;

    private final DataSetService dataSetService;

    private final DateInfoRepository dateInfoRepository;

    private final ModelRelaService modelRelaService;

    private final ApplicationEventPublisher eventPublisher;

    ExecutorService executor =
            new ThreadPoolExecutor(0, 5, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public ModelServiceImpl(ModelRepository modelRepository, DatabaseService databaseService,
            @Lazy DimensionService dimensionService, @Lazy MetricService metricService,
            DomainService domainService, UserService userService, DataSetService dataSetService,
            DateInfoRepository dateInfoRepository, ModelRelaService modelRelaService,
            ApplicationEventPublisher eventPublisher) {
        this.modelRepository = modelRepository;
        this.databaseService = databaseService;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.domainService = domainService;
        this.userService = userService;
        this.dataSetService = dataSetService;
        this.dateInfoRepository = dateInfoRepository;
        this.modelRelaService = modelRelaService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ModelResp createModel(ModelReq modelReq, User user) throws Exception {
        // checkParams(modelReq);
        ModelDO modelDO = ModelConverter.convert(modelReq, user);
        modelRepository.createModel(modelDO);
        batchCreateDimension(modelDO, user);
        batchCreateMetric(modelDO, user);
        sendEvent(modelDO, EventType.ADD);
        return ModelConverter.convert(modelDO);
    }

    @Override
    public List<ModelResp> createModel(ModelBuildReq modelBuildReq, User user) throws Exception {
        List<ModelResp> modelResps = Lists.newArrayList();
        Map<String, ModelSchema> modelSchemaMap = buildModelSchema(modelBuildReq);
        for (Map.Entry<String, ModelSchema> entry : modelSchemaMap.entrySet()) {
            ModelReq modelReq =
                    ModelConverter.convert(entry.getValue(), modelBuildReq, entry.getKey());
            ModelResp modelResp = createModel(modelReq, user);
            modelResps.add(modelResp);
        }
        return modelResps;
    }

    @Override
    @Transactional
    public ModelResp updateModel(ModelReq modelReq, User user) throws Exception {
        // Comment out below checks for now, they seem unnecessary and
        // lead to unexpected exception in updating model
        /*
         * checkParams(modelReq); checkRelations(modelReq);
         */
        ModelDO modelDO = modelRepository.getModelById(modelReq.getId());
        ModelConverter.convert(modelDO, modelReq, user);
        modelRepository.updateModel(modelDO);
        batchCreateDimension(modelDO, user);
        batchCreateMetric(modelDO, user);
        sendEvent(modelDO, EventType.UPDATE);
        return ModelConverter.convert(modelDO);
    }

    @Override
    public List<ModelResp> getModelList(MetaFilter metaFilter) {
        ModelFilter modelFilter = new ModelFilter();
        BeanUtils.copyProperties(metaFilter, modelFilter);
        List<ModelResp> modelResps =
                ModelConverter.convertList(modelRepository.getModelList(modelFilter));
        if (modelFilter.getDataSetId() != null) {
            DataSetResp dataSetResp = dataSetService.getDataSet(modelFilter.getDataSetId());
            return modelResps.stream()
                    .filter(modelResp -> dataSetResp.getAllModels().contains(modelResp.getId()))
                    .collect(Collectors.toList());
        }
        return modelResps;
    }

    @Override
    public Map<Long, ModelResp> getModelMap(ModelFilter modelFilter) {
        Map<Long, ModelResp> map = new HashMap<>();
        List<ModelResp> modelResps = getModelList(modelFilter);
        if (CollectionUtils.isEmpty(modelResps)) {
            return map;
        }
        return modelResps.stream()
                .collect(Collectors.toMap(ModelResp::getId, a -> a, (k1, k2) -> k1));
    }

    @Override
    public void deleteModel(Long id, User user) {
        ModelDO datasourceDO = modelRepository.getModelById(id);
        if (datasourceDO == null) {
            return;
        }
        checkDelete(id);
        datasourceDO.setStatus(StatusEnum.DELETED.getCode());
        datasourceDO.setUpdatedAt(new Date());
        datasourceDO.setUpdatedBy(user.getName());
        modelRepository.updateModel(datasourceDO);
    }

    @Override
    public ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric) {
        List<DateInfoReq> itemDates = new ArrayList<>();
        List<DateInfoDO> dimensions = dateInfoRepository.getDateInfos(dimension);
        List<DateInfoDO> metrics = dateInfoRepository.getDateInfos(metric);

        log.info("getDateDate, dimension:{}, dimensions dateInfo:{}", dimension, dimensions);
        log.info("getDateDate, metric:{}, metrics dateInfo:{}", metric, metrics);
        itemDates.addAll(convert(dimensions));
        itemDates.addAll(convert(metrics));

        ItemDateResp itemDateDescriptor = calculateDateInternal(itemDates);
        log.info("itemDateDescriptor:{}", itemDateDescriptor);

        return itemDateDescriptor;
    }

    @Override
    public UnAvailableItemResp getUnAvailableItem(FieldRemovedReq fieldRemovedReq) {
        if (CollectionUtils.isEmpty(fieldRemovedReq.getFields())) {
            return UnAvailableItemResp.builder().build();
        }
        MetaFilter metaFilter = new MetaFilter(Lists.newArrayList(fieldRemovedReq.getModelId()));
        metaFilter.setFieldsDepend(fieldRemovedReq.getFields());
        List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
        List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
        return UnAvailableItemResp.builder().dimensionResps(dimensionResps).metricResps(metricResps)
                .build();
    }

    @Override
    public Map<String, ModelSchema> buildModelSchema(ModelBuildReq modelBuildReq)
            throws SQLException {
        List<DbSchema> dbSchemas = getDbSchemes(modelBuildReq);
        Map<String, ModelSchema> modelSchemaMap = new ConcurrentHashMap<>();
        CompletableFuture.allOf(dbSchemas.stream()
                .map(dbSchema -> CompletableFuture.runAsync(
                        () -> doBuild(modelBuildReq, dbSchema, dbSchemas, modelSchemaMap),
                        executor))
                .toArray(CompletableFuture[]::new)).join();
        return modelSchemaMap;
    }

    private void doBuild(ModelBuildReq modelBuildReq, DbSchema curSchema, List<DbSchema> dbSchemas,
            Map<String, ModelSchema> modelSchemaMap) {
        ModelSchema modelSchema = new ModelSchema();
        List<SemanticModeller> semanticModellers = CoreComponentFactory.getSemanticModellers();
        for (SemanticModeller semanticModeller : semanticModellers) {
            semanticModeller.build(curSchema, dbSchemas, modelSchema, modelBuildReq);
        }
        modelSchemaMap.put(curSchema.getTable(), modelSchema);
    }

    private List<DbSchema> getDbSchemes(ModelBuildReq modelBuildReq) throws SQLException {
        if (!CollectionUtils.isEmpty(modelBuildReq.getDbSchemas())) {
            return modelBuildReq.getDbSchemas();
        }
        Map<String, List<DBColumn>> dbColumnMap = databaseService.getDbColumns(modelBuildReq);
        return convert(dbColumnMap, modelBuildReq);
    }

    private List<DbSchema> convert(Map<String, List<DBColumn>> dbColumnMap,
            ModelBuildReq modelBuildReq) {
        return dbColumnMap.keySet().stream()
                .map(key -> convert(modelBuildReq, key, dbColumnMap.get(key)))
                .collect(Collectors.toList());
    }

    private DbSchema convert(ModelBuildReq modelSchemaReq, String key, List<DBColumn> dbColumns) {
        DbSchema dbSchema = new DbSchema();
        dbSchema.setDb(modelSchemaReq.getDb());
        dbSchema.setTable(key);
        dbSchema.setSql(modelSchemaReq.getSql());
        dbSchema.setDbColumns(dbColumns);
        return dbSchema;
    }

    private void batchCreateDimension(ModelDO modelDO, User user) throws Exception {
        List<DimensionReq> dimensionReqs = ModelConverter.convertDimensionList(modelDO);
        dimensionService.createDimensionBatch(dimensionReqs, user);
    }

    private void batchCreateMetric(ModelDO datasourceDO, User user) throws Exception {
        List<MetricReq> metricReqs = ModelConverter.convertMetricList(datasourceDO);
        metricService.createMetricBatch(metricReqs, user);
    }

    private void checkParams(ModelReq modelReq) {
        String forbiddenCharacters = NameCheckUtils.findForbiddenCharacters(modelReq.getName());
        if (StringUtils.isNotBlank(forbiddenCharacters)) {
            String message = String.format("模型名称[%s]包含特殊字符(%s), 请修改", modelReq.getName(),
                    forbiddenCharacters);
            throw new InvalidArgumentException(message);
        }

        if (!NameCheckUtils.isValidIdentifier(modelReq.getBizName())) {
            String message = String.format("模型英文名[%s]需要为下划线字母数字组合, 请修改", modelReq.getBizName());
            throw new InvalidArgumentException(message);
        }
        if (modelReq.getModelDetail() == null) {
            return;
        }
        List<Dimension> dims = modelReq.getModelDetail().getDimensions();
        List<Measure> measures = modelReq.getModelDetail().getMeasures();
        List<Identify> identifies = modelReq.getModelDetail().getIdentifiers();
        for (Measure measure : measures) {
            String measureForbiddenCharacters =
                    NameCheckUtils.findForbiddenCharacters(measure.getName());
            if (StringUtils.isNotBlank(measure.getName())
                    && StringUtils.isNotBlank(measureForbiddenCharacters)) {
                String message = String.format("度量[%s]包含特殊字符(%s), 请修改", measure.getName(),
                        measureForbiddenCharacters);
                throw new InvalidArgumentException(message);
            }
        }
        for (Dimension dim : dims) {
            String dimForbiddenCharacters = NameCheckUtils.findForbiddenCharacters(dim.getName());
            if (StringUtils.isNotBlank(dim.getName())
                    && StringUtils.isNotBlank(dimForbiddenCharacters)) {
                String message = String.format("维度[%s]包含特殊字符(%s), 请修改", dim.getName(),
                        dimForbiddenCharacters);
                throw new InvalidArgumentException(message);
            }
        }
        for (Identify identify : identifies) {
            String identifyForbiddenCharacters =
                    NameCheckUtils.findForbiddenCharacters(identify.getName());
            if (StringUtils.isNotBlank(identify.getName())
                    && StringUtils.isNotBlank(identifyForbiddenCharacters)) {
                String message = String.format("主键/外键[%s]包含特殊字符(%s), 请修改", identify.getName(),
                        identifyForbiddenCharacters);
                throw new InvalidArgumentException(message);
            }
        }
    }

    private void checkRelations(ModelReq modelReq) {
        List<ModelRela> modelRelas = modelRelaService.getModelRela(Arrays.asList(modelReq.getId()));
        if (CollectionUtils.isEmpty(modelRelas)) {
            return;
        }
        Set<String> relations = new HashSet<>();
        for (ModelRela modelRela : modelRelas) {
            if (modelRela.getFromModelId().equals(modelReq.getId())) {
                modelRela.getJoinConditions().forEach(r -> relations.add(r.getLeftField()));
            }
            if (modelRela.getToModelId().equals(modelReq.getId())) {
                modelRela.getJoinConditions().forEach(r -> relations.add(r.getRightField()));
            }
        }
        if (relations.isEmpty()) {
            return;
        }
        // any identify in model relation should not be deleted
        if (modelReq.getModelDetail() == null
                || CollectionUtils.isEmpty(modelReq.getModelDetail().getIdentifiers())) {
            throw new InvalidArgumentException("模型关联中主键/外键不存在, 请检查");
        }
        List<String> modelIdentifiers = modelReq.getModelDetail().getIdentifiers().stream()
                .map(Identify::getBizName).collect(Collectors.toList());
        for (String rela : relations) {
            if (!modelIdentifiers.contains(rela)) {
                throw new InvalidArgumentException(String.format("模型关联中主键/外键(%s)不存在, 请检查", rela));
            }
        }
    }

    private void checkDelete(Long modelId) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setModelIds(Lists.newArrayList(modelId));
        List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
        List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
        boolean validMetric = metricResps.stream().anyMatch(
                metricResp -> Objects.equals(metricResp.getStatus(), StatusEnum.ONLINE.getCode()));
        boolean validDimension = dimensionResps.stream().anyMatch(dimensionResp -> Objects
                .equals(dimensionResp.getStatus(), StatusEnum.ONLINE.getCode()));
        if (validMetric || validDimension) {
            throw new RuntimeException("存在基于该模型创建的指标和维度, 暂不能删除, 请确认");
        }
    }

    private ItemDateResp calculateDateInternal(List<DateInfoReq> itemDates) {
        if (CollectionUtils.isEmpty(itemDates)) {
            log.warn("itemDates is empty!");
            return null;
        }
        String dateFormat = itemDates.get(0).getDateFormat();
        String startDate = itemDates.get(0).getStartDate();
        String endDate = itemDates.get(0).getEndDate();
        String datePeriod = itemDates.get(0).getDatePeriod();
        List<String> unavailableDateList = itemDates.get(0).getUnavailableDateList();
        for (DateInfoReq item : itemDates) {
            String startDate1 = item.getStartDate();
            String endDate1 = item.getEndDate();
            List<String> unavailableDateList1 = item.getUnavailableDateList();
            if (StringUtils.isNotEmpty(startDate1) && startDate1.compareTo(startDate) > 0) {
                startDate = startDate1;
            }
            if (StringUtils.isNotEmpty(endDate1) && endDate1.compareTo(endDate) < 0) {
                endDate = endDate1;
            }
            if (!CollectionUtils.isEmpty(unavailableDateList1)) {
                unavailableDateList.addAll(unavailableDateList1);
            }
        }

        return new ItemDateResp(dateFormat, startDate, endDate, datePeriod, unavailableDateList);
    }

    @Override
    public List<ModelResp> getModelListWithAuth(User user, Long domainId, AuthType authType) {
        List<ModelResp> modelResps = getModelAuthList(user, domainId, authType);
        Set<ModelResp> modelRespSet = new HashSet<>(modelResps);
        List<ModelResp> modelRespsAuthInheritDomain =
                getModelRespAuthInheritDomain(user, domainId, authType);
        modelRespSet.addAll(modelRespsAuthInheritDomain);
        return modelRespSet.stream().sorted(Comparator.comparingLong(ModelResp::getId))
                .collect(Collectors.toList());
    }

    public List<ModelResp> getModelRespAuthInheritDomain(User user, Long domainId,
            AuthType authType) {
        List<Long> domainIds =
                domainService.getDomainAuthSet(user, authType).stream().filter(domainResp -> {
                    if (domainId == null) {
                        return true;
                    } else {
                        return domainId.equals(domainResp.getId())
                                || domainId.equals(domainResp.getParentId());
                    }
                }).map(DomainResp::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(domainIds)) {
            return Lists.newArrayList();
        }
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setIncludesDetail(false);
        modelFilter.setDomainIds(domainIds);
        return getModelList(modelFilter);
    }

    @Override
    public List<ModelResp> getModelAuthList(User user, Long domainId, AuthType authTypeEnum) {
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setIncludesDetail(false);
        modelFilter.setDomainId(domainId);
        List<ModelResp> modelResps = getModelList(modelFilter);
        Set<String> orgIds = userService.getUserAllOrgId(user.getName());
        List<ModelResp> modelWithAuth = Lists.newArrayList();
        if (authTypeEnum.equals(AuthType.ADMIN)) {
            modelWithAuth = modelResps.stream()
                    .filter(modelResp -> checkAdminPermission(orgIds, user, modelResp))
                    .collect(Collectors.toList());
        }
        if (authTypeEnum.equals(AuthType.VIEWER)) {
            modelWithAuth = modelResps.stream()
                    .filter(domainResp -> checkDataSetPermission(orgIds, user, domainResp))
                    .collect(Collectors.toList());
        }
        return modelWithAuth;
    }

    @Override
    public List<ModelResp> getModelByDomainIds(List<Long> domainIds) {
        if (CollectionUtils.isEmpty(domainIds)) {
            return Lists.newArrayList();
        }
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setDomainIds(domainIds);
        modelFilter.setIncludesDetail(true);
        List<ModelResp> modelResps = getModelList(modelFilter);
        if (CollectionUtils.isEmpty(modelResps)) {
            return modelResps;
        }
        return modelResps.stream().filter(modelResp -> domainIds.contains(modelResp.getDomainId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelResp> getAllModelByDomainIds(List<Long> domainIds) {
        Set<DomainResp> domainResps = domainService.getDomainChildren(domainIds);
        List<Long> allDomainIds =
                domainResps.stream().map(DomainResp::getId).collect(Collectors.toList());
        return getModelByDomainIds(allDomainIds);
    }

    @Override
    public ModelResp getModel(Long id) {
        ModelDO modelDO = getModelDO(id);
        if (modelDO == null) {
            return null;
        }

        DomainResp domainResp = domainService.getDomain(modelDO.getDomainId());
        return ModelConverter.convert(modelDO, domainResp);
    }

    @Override
    public List<String> getModelAdmin(Long id) {
        ModelResp modelResp = getModel(id);
        if (modelResp == null) {
            return Lists.newArrayList();
        }
        if (!CollectionUtils.isEmpty(modelResp.getAdmins())) {
            return modelResp.getAdmins();
        }
        Long domainId = modelResp.getDomainId();
        DomainResp domainResp = domainService.getDomain(domainId);
        while (domainResp != null) {
            if (!CollectionUtils.isEmpty(domainResp.getAdmins())) {
                return domainResp.getAdmins();
            }
            domainId = domainResp.getParentId();
            domainResp = domainService.getDomain(domainId);
        }
        return Lists.newArrayList();
    }

    @Override
    public DatabaseResp getDatabaseByModelId(Long modelId) {
        ModelResp modelResp = getModel(modelId);
        if (modelResp != null) {
            Long databaseId = modelResp.getDatabaseId();
            return databaseService.getDatabase(databaseId);
        }
        return null;
    }

    @Override
    public void batchUpdateStatus(MetaBatchReq metaBatchReq, User user) {
        if (CollectionUtils.isEmpty(metaBatchReq.getIds())) {
            return;
        }
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setIds(metaBatchReq.getIds());
        List<ModelDO> modelDOS = modelRepository.getModelList(modelFilter);
        if (CollectionUtils.isEmpty(modelDOS)) {
            return;
        }
        modelDOS = modelDOS.stream().peek(modelDO -> {
            modelDO.setStatus(metaBatchReq.getStatus());
            modelDO.setUpdatedAt(new Date());
            modelDO.setUpdatedBy(user.getName());
            if (StatusEnum.OFFLINE.getCode().equals(metaBatchReq.getStatus())
                    || StatusEnum.DELETED.getCode().equals(metaBatchReq.getStatus())) {
                metricService.sendMetricEventBatch(Lists.newArrayList(modelDO.getId()),
                        EventType.DELETE);
                dimensionService.sendDimensionEventBatch(Lists.newArrayList(modelDO.getId()),
                        EventType.DELETE);
            } else if (StatusEnum.ONLINE.getCode().equals(metaBatchReq.getStatus())) {
                metricService.sendMetricEventBatch(Lists.newArrayList(modelDO.getId()),
                        EventType.ADD);
                dimensionService.sendDimensionEventBatch(Lists.newArrayList(modelDO.getId()),
                        EventType.ADD);
            }
        }).collect(Collectors.toList());
        modelRepository.batchUpdate(modelDOS);
    }

    protected ModelDO getModelDO(Long id) {
        return modelRepository.getModelById(id);
    }

    private List<DateInfoReq> convert(List<DateInfoDO> dateInfoDOList) {
        List<DateInfoReq> dateInfoCommendList = new ArrayList<>();
        dateInfoDOList.forEach(dateInfoDO -> {
            DateInfoReq dateInfoCommend = new DateInfoReq();
            BeanUtils.copyProperties(dateInfoDO, dateInfoCommend);
            dateInfoCommend.setUnavailableDateList(
                    JsonUtil.toList(dateInfoDO.getUnavailableDateList(), String.class));
            dateInfoCommendList.add(dateInfoCommend);
        });
        return dateInfoCommendList;
    }

    public static boolean checkAdminPermission(Set<String> orgIds, User user, ModelResp modelResp) {
        List<String> admins = modelResp.getAdmins();
        List<String> adminOrgs = modelResp.getAdminOrgs();
        if (user.isSuperAdmin()) {
            return true;
        }
        String userName = user.getName();
        if (admins.contains(userName) || modelResp.getCreatedBy().equals(userName)) {
            return true;
        }
        if (CollectionUtils.isEmpty(adminOrgs)) {
            return false;
        }
        for (String orgId : orgIds) {
            if (adminOrgs.contains(orgId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkDataSetPermission(Set<String> orgIds, User user,
            ModelResp modelResp) {
        if (checkAdminPermission(orgIds, user, modelResp)) {
            return true;
        }
        List<String> viewers = modelResp.getViewers();
        List<String> viewOrgs = modelResp.getViewOrgs();
        if (user.isSuperAdmin()) {
            return true;
        }
        if (modelResp.openToAll()) {
            return true;
        }
        String userName = user.getName();
        if (viewers.contains(userName) || modelResp.getCreatedBy().equals(userName)) {
            return true;
        }
        if (CollectionUtils.isEmpty(viewOrgs)) {
            return false;
        }
        for (String orgId : orgIds) {
            if (viewOrgs.contains(orgId)) {
                return true;
            }
        }
        return false;
    }

    private void sendEvent(ModelDO modelDO, EventType eventType) {
        DataItem dataItem = getDataItem(modelDO);
        eventPublisher.publishEvent(new DataEvent(this, Lists.newArrayList(dataItem), eventType));
    }

    private DataItem getDataItem(ModelDO modelDO) {
        return DataItem.builder().id(modelDO.getId().toString()).name(modelDO.getName())
                .bizName(modelDO.getBizName()).modelId(modelDO.getId().toString())
                .domainId(modelDO.getDomainId().toString()).type(TypeEnums.MODEL).build();
    }

}
