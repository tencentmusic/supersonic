package com.tencent.supersonic.semantic.model.application;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.RelateDimension;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.request.DateInfoReq;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.MetaBatchReq;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.model.yaml.DataModelYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelRelaService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.dataobject.DateInfoDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import com.tencent.supersonic.semantic.model.domain.manager.DatasourceYamlManager;
import com.tencent.supersonic.semantic.model.domain.manager.DimensionYamlManager;
import com.tencent.supersonic.semantic.model.domain.manager.MetricYamlManager;
import com.tencent.supersonic.semantic.model.domain.pojo.Datasource;
import com.tencent.supersonic.semantic.model.domain.pojo.DimensionFilter;
import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.model.domain.pojo.ModelFilter;
import com.tencent.supersonic.semantic.model.domain.repository.DateInfoRepository;
import com.tencent.supersonic.semantic.model.domain.repository.ModelRepository;
import com.tencent.supersonic.semantic.model.domain.utils.DimensionConverter;
import com.tencent.supersonic.semantic.model.domain.utils.MetricConverter;
import com.tencent.supersonic.semantic.model.domain.utils.ModelConverter;
import com.tencent.supersonic.semantic.model.domain.utils.NameCheckUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ModelServiceImpl implements ModelService {

    private ModelRepository modelRepository;

    private DatabaseService databaseService;

    private DimensionService dimensionService;

    private MetricService metricService;

    private DomainService domainService;

    private ModelRelaService modelRelaService;

    private UserService userService;

    private DateInfoRepository dateInfoRepository;

    public ModelServiceImpl(ModelRepository modelRepository,
                            DatabaseService databaseService,
                            @Lazy DimensionService dimensionService,
                            @Lazy MetricService metricService,
                            ModelRelaService modelRelaService,
                            DomainService domainService,
                            UserService userService,
                            DateInfoRepository dateInfoRepository) {
        this.modelRepository = modelRepository;
        this.databaseService = databaseService;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.domainService = domainService;
        this.modelRelaService = modelRelaService;
        this.userService = userService;
        this.dateInfoRepository = dateInfoRepository;
    }

    @Override
    @Transactional
    public ModelResp createModel(ModelReq modelReq, User user) throws Exception {
        checkName(modelReq);
        ModelDO datasourceDO = ModelConverter.convert(modelReq, user);
        modelRepository.createModel(datasourceDO);
        batchCreateDimension(datasourceDO, user);
        batchCreateMetric(datasourceDO, user);
        return ModelConverter.convert(datasourceDO);
    }

    @Override
    @Transactional
    public ModelResp updateModel(ModelReq modelReq, User user) throws Exception {
        checkName(modelReq);
        ModelDO modelDO = modelRepository.getModelById(modelReq.getId());
        int oldStatus = modelDO.getStatus();
        ModelConverter.convert(modelDO, modelReq, user);
        modelRepository.updateModel(modelDO);
        batchCreateDimension(modelDO, user);
        batchCreateMetric(modelDO, user);
        statusPublish(oldStatus, modelDO);
        return ModelConverter.convert(modelDO);
    }

    private void statusPublish(Integer oldStatus, ModelDO modelDO) {
        if (oldStatus.equals(modelDO.getStatus())) {
            return;
        }
        EventType eventType = null;
        if (oldStatus.equals(StatusEnum.ONLINE.getCode())
                && modelDO.getStatus().equals(StatusEnum.OFFLINE.getCode())) {
            eventType = EventType.DELETE;
        } else if (oldStatus.equals(StatusEnum.OFFLINE.getCode())
                && modelDO.getStatus().equals(StatusEnum.ONLINE.getCode())) {
            eventType = EventType.ADD;
        }
        log.info("model:{} status from {} to {}", modelDO.getId(), oldStatus, modelDO.getStatus());
        metricService.sendMetricEventBatch(Lists.newArrayList(modelDO.getId()), eventType);
        dimensionService.sendDimensionEventBatch(Lists.newArrayList(modelDO.getId()), eventType);
    }

    @Override
    public List<ModelResp> getModelList(ModelFilter modelFilter) {
        return ModelConverter.convertList(modelRepository.getModelList(modelFilter));
    }

    @Override
    public Map<Long, ModelResp> getModelMap() {
        Map<Long, ModelResp> map = new HashMap<>();
        List<ModelResp> modelResps = getModelList(new ModelFilter());
        if (CollectionUtils.isEmpty(modelResps)) {
            return map;
        }
        return modelResps.stream().collect(Collectors.toMap(ModelResp::getId, a -> a, (k1, k2) -> k1));
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
    public List<MeasureResp> getMeasureListOfModel(List<Long> modelIds) {
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setIds(modelIds);
        List<ModelResp> modelResps = getModelList(modelFilter);
        return modelResps.stream().flatMap(modelResp -> modelResp.getModelDetail().getMeasures()
                .stream().map(measure -> ModelConverter.convert(measure, modelResp))).collect(Collectors.toList());
    }

    private void batchCreateDimension(ModelDO datasourceDO, User user) throws Exception {
        List<DimensionReq> dimensionReqs = ModelConverter.convertDimensionList(datasourceDO);
        dimensionService.createDimensionBatch(dimensionReqs, user);
    }

    private void batchCreateMetric(ModelDO datasourceDO, User user) throws Exception {
        List<MetricReq> exprMetricReqs = ModelConverter.convertMetricList(datasourceDO);
        metricService.createMetricBatch(exprMetricReqs, user);
    }

    private void checkName(ModelReq modelReq) {
        if (NameCheckUtils.containsSpecialCharacters(modelReq.getName())) {
            String message = String.format("模型名称[%s]包含特殊字符, 请修改", modelReq.getName());
            throw new InvalidArgumentException(message);
        }
        List<Dim> dims = modelReq.getModelDetail().getDimensions();
        List<Measure> measures = modelReq.getModelDetail().getMeasures();
        List<Dim> timeDims = modelReq.getTimeDimension();
        List<Identify> identifies = modelReq.getModelDetail().getIdentifiers();
        if (CollectionUtils.isEmpty(dims)) {
            throw new InvalidArgumentException("缺少维度信息");
        }
        if (!CollectionUtils.isEmpty(measures) && CollectionUtils.isEmpty(timeDims)) {
            throw new InvalidArgumentException("有度量时, 不可缺少时间维度");
        }
        for (Measure measure : measures) {
            if (StringUtils.isNotBlank(measure.getName())
                    && NameCheckUtils.containsSpecialCharacters(measure.getName())) {
                String message = String.format("度量[%s]包含特殊字符, 请修改", measure.getName());
                throw new InvalidArgumentException(message);
            }
        }
        for (Dim dim : dims) {
            if (StringUtils.isNotBlank(dim.getName())
                    && NameCheckUtils.containsSpecialCharacters(dim.getName())) {
                String message = String.format("维度[%s]包含特殊字符, 请修改", dim.getName());
                throw new InvalidArgumentException(message);
            }
        }
        for (Identify identify : identifies) {
            if (StringUtils.isNotBlank(identify.getName())
                    && NameCheckUtils.containsSpecialCharacters(identify.getName())) {
                String message = String.format("主键/外键[%s]包含特殊字符, 请修改", identify.getName());
                throw new InvalidArgumentException(message);
            }
        }
    }

    private void checkDelete(Long modelId) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setModelIds(Lists.newArrayList(modelId));
        List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
        List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
        if (!CollectionUtils.isEmpty(metricResps) || !CollectionUtils.isEmpty(dimensionResps)) {
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
            if (Strings.isNotEmpty(startDate1) && startDate1.compareTo(startDate) > 0) {
                startDate = startDate1;
            }
            if (Strings.isNotEmpty(endDate1) && endDate1.compareTo(endDate) < 0) {
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
        List<ModelResp> modelResps = getModelAuthList(user, authType);
        Set<ModelResp> modelRespSet = new HashSet<>(modelResps);
        List<ModelResp> modelRespsAuthInheritDomain = getModelRespAuthInheritDomain(user, authType);
        modelRespSet.addAll(modelRespsAuthInheritDomain);
        if (domainId != null && domainId > 0) {
            modelRespSet = modelRespSet.stream().filter(modelResp ->
                    modelResp.getDomainId().equals(domainId)).collect(Collectors.toSet());
        }
        return fillMetricInfo(new ArrayList<>(modelRespSet)).stream()
                .sorted(Comparator.comparingLong(SchemaItem::getId)).collect(Collectors.toList());
    }

    public List<ModelResp> getModelRespAuthInheritDomain(User user, AuthType authType) {
        Set<DomainResp> domainResps = domainService.getDomainAuthSet(user, authType);
        if (CollectionUtils.isEmpty(domainResps)) {
            return Lists.newArrayList();
        }
        List<ModelResp> allModelList = getModelList(new ModelFilter());
        Set<Long> domainIds = domainResps.stream().map(DomainResp::getId).collect(Collectors.toSet());
        return allModelList.stream().filter(modelResp ->
                domainIds.contains(modelResp.getDomainId())).collect(Collectors.toList());
    }

    @Override
    public List<ModelResp> getModelAuthList(User user, AuthType authTypeEnum) {
        List<ModelResp> modelResps = getModelList(new ModelFilter());
        Set<String> orgIds = userService.getUserAllOrgId(user.getName());
        List<ModelResp> modelWithAuth = Lists.newArrayList();
        if (authTypeEnum.equals(AuthType.ADMIN)) {
            modelWithAuth = modelResps.stream()
                    .filter(modelResp -> checkAdminPermission(orgIds, user, modelResp))
                    .collect(Collectors.toList());
        }
        if (authTypeEnum.equals(AuthType.VISIBLE)) {
            modelWithAuth = modelResps.stream()
                    .filter(domainResp -> checkViewerPermission(orgIds, user, domainResp))
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
        List<ModelResp> modelResps = getModelList(modelFilter);
        if (CollectionUtils.isEmpty(modelResps)) {
            return modelResps;
        }
        return modelResps.stream().filter(modelResp ->
                domainIds.contains(modelResp.getDomainId())).collect(Collectors.toList());
    }

    @Override
    public ModelResp getModel(Long id) {
        ModelDO modelDO = getModelDO(id);
        if (modelDO == null) {
            return null;
        }
        Map<Long, DomainResp> domainRespMap = domainService.getDomainList().stream()
                .collect(Collectors.toMap(DomainResp::getId, d -> d));
        return ModelConverter.convert(modelDO, domainRespMap);
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
    public ModelSchemaResp fetchSingleModelSchema(Long modelId) {
        ModelResp modelResp = getModel(modelId);
        ModelSchemaResp modelSchemaResp = new ModelSchemaResp();
        BeanUtils.copyProperties(modelResp, modelSchemaResp);
        modelSchemaResp.setDimensions(generateDimSchema(modelId));
        modelSchemaResp.setMetrics(generateMetricSchema(modelId, modelResp));
        return modelSchemaResp;
    }

    @Override
    public List<ModelSchemaResp> fetchModelSchema(ModelSchemaFilterReq modelSchemaFilterReq) {
        List<ModelSchemaResp> modelSchemaRespList = new ArrayList<>();
        List<Long> modelIds = modelSchemaFilterReq.getModelIds();
        if (CollectionUtils.isEmpty(modelIds)) {
            modelIds = generateModelIdsReq(modelSchemaFilterReq);
        }
        MetaFilter metaFilter = new MetaFilter(modelIds);
        metaFilter.setStatus(StatusEnum.ONLINE.getCode());
        Map<Long, List<MetricResp>> metricRespMap = metricService.getMetrics(metaFilter)
                .stream().collect(Collectors.groupingBy(MetricResp::getModelId));
        Map<Long, List<DimensionResp>> dimensionRespsMap = dimensionService.getDimensions(metaFilter)
                .stream().collect(Collectors.groupingBy(DimensionResp::getModelId));
        List<ModelRela> modelRelas = modelRelaService.getModelRela(modelIds);
        for (Long modelId : modelIds) {
            ModelResp modelResp = getModelMap().get(modelId);
            if (modelResp == null || !StatusEnum.ONLINE.getCode().equals(modelResp.getStatus())) {
                continue;
            }
            List<MetricResp> metricResps = metricRespMap.getOrDefault(modelId, Lists.newArrayList());
            List<MetricSchemaResp> metricSchemaResps = metricResps.stream().map(metricResp ->
                    convert(metricResp, modelResp)).collect(Collectors.toList());
            List<DimSchemaResp> dimensionResps = dimensionRespsMap.getOrDefault(modelId, Lists.newArrayList())
                    .stream().map(this::convert).collect(Collectors.toList());
            ModelSchemaResp modelSchemaResp = new ModelSchemaResp();
            BeanUtils.copyProperties(modelResp, modelSchemaResp);
            modelSchemaResp.setDimensions(dimensionResps);
            modelSchemaResp.setMetrics(metricSchemaResps);
            modelSchemaResp.setModelRelas(modelRelas.stream().filter(modelRela
                    -> modelRela.getFromModelId().equals(modelId) || modelRela.getToModelId().equals(modelId))
                    .collect(Collectors.toList()));
            modelSchemaRespList.add(modelSchemaResp);
        }
        return modelSchemaRespList;
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
        modelDOS = modelDOS.stream()
                .peek(modelDO -> {
                    modelDO.setStatus(metaBatchReq.getStatus());
                    modelDO.setUpdatedAt(new Date());
                    modelDO.setUpdatedBy(user.getName());
                })
                .collect(Collectors.toList());
        modelRepository.batchUpdate(modelDOS);
    }

    private List<ModelResp> fillMetricInfo(List<ModelResp> modelResps) {
        if (CollectionUtils.isEmpty(modelResps)) {
            return modelResps;
        }
        Map<Long, List<MetricResp>> metricMap = metricService.getMetrics(new MetricFilter()).stream()
                .collect(Collectors.groupingBy(MetricResp::getModelId));
        Map<Long, List<DimensionResp>> dimensionMap = dimensionService.getDimensions(new DimensionFilter()).stream()
                .collect(Collectors.groupingBy(DimensionResp::getModelId));
        modelResps.forEach(modelResp -> {
            modelResp.setDimensionCnt(dimensionMap.getOrDefault(modelResp.getId(), Lists.newArrayList()).size());
            modelResp.setMetricCnt(metricMap.getOrDefault(modelResp.getId(), Lists.newArrayList()).size());
        });
        return modelResps;
    }

    protected ModelDO getModelDO(Long id) {
        return modelRepository.getModelById(id);
    }

    private List<MetricSchemaResp> generateMetricSchema(Long modelId, ModelResp modelResp) {
        List<MetricSchemaResp> metricSchemaDescList = new ArrayList<>();
        List<MetricResp> metricResps = metricService.getMetrics(new MetaFilter(Lists.newArrayList(modelId)));
        metricResps.forEach(metricResp -> metricSchemaDescList.add(convert(metricResp, modelResp)));
        return metricSchemaDescList;
    }

    private List<DimSchemaResp> generateDimSchema(Long modelId) {
        List<DimensionResp> dimDescList = dimensionService.getDimensions(new MetaFilter(Lists.newArrayList(modelId)));
        return dimDescList.stream().map(this::convert).collect(Collectors.toList());
    }

    private DimSchemaResp convert(DimensionResp dimensionResp) {
        DimSchemaResp dimSchemaResp = new DimSchemaResp();
        BeanUtils.copyProperties(dimensionResp, dimSchemaResp);
        dimSchemaResp.setUseCnt(0L);
        return dimSchemaResp;
    }

    private MetricSchemaResp convert(MetricResp metricResp, ModelResp modelResp) {
        MetricSchemaResp metricSchemaResp = new MetricSchemaResp();
        BeanUtils.copyProperties(metricResp, metricSchemaResp);
        RelateDimension relateDimension = metricResp.getRelateDimension();
        if (relateDimension == null || CollectionUtils.isEmpty(relateDimension.getDrillDownDimensions())) {
            metricSchemaResp.setRelateDimension(RelateDimension.builder()
                    .drillDownDimensions(modelResp.getDrillDownDimensions()).build());
        }
        metricSchemaResp.setUseCnt(0L);
        return metricSchemaResp;
    }

    private List<DateInfoReq> convert(List<DateInfoDO> dateInfoDOList) {
        List<DateInfoReq> dateInfoCommendList = new ArrayList<>();
        dateInfoDOList.forEach(dateInfoDO -> {
            DateInfoReq dateInfoCommend = new DateInfoReq();
            BeanUtils.copyProperties(dateInfoDO, dateInfoCommend);
            dateInfoCommend.setUnavailableDateList(JsonUtil.toList(dateInfoDO.getUnavailableDateList(), String.class));
            dateInfoCommendList.add(dateInfoCommend);
        });
        return dateInfoCommendList;
    }

    private List<Long> generateModelIdsReq(ModelSchemaFilterReq filter) {
        if (Objects.nonNull(filter) && !CollectionUtils.isEmpty(filter.getModelIds())) {
            return filter.getModelIds();
        }
        return new ArrayList<>(getModelMap().keySet());
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

    public static boolean checkViewerPermission(Set<String> orgIds, User user, ModelResp modelResp) {
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

    @Override
    public void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DataModelYamlTpl> dataModelYamlTplList, List<MetricYamlTpl> metricYamlTplList,
            Map<Long, String> modelIdName) {
        for (Long modelId : modelIds) {
            ModelResp modelResp = getModel(modelId);
            modelIdName.put(modelId, modelResp.getBizName());
            MetaFilter metaFilter = new MetaFilter(Lists.newArrayList(modelId));
            List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
            metricYamlTplList.addAll(MetricYamlManager.convert2YamlObj(MetricConverter.metricInfo2Metric(metricResps)));
            Long databaseId = modelResp.getDatabaseId();
            DatabaseResp databaseResp = databaseService.getDatabase(databaseId);
            List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);

            dataModelYamlTplList.add(DatasourceYamlManager.convert2YamlObj(
                    datasourceInfo2Datasource(modelResp), databaseResp));
            if (!dimensionYamlMap.containsKey(modelResp.getBizName())) {
                dimensionYamlMap.put(modelResp.getBizName(), new ArrayList<>());
            }
            List<DimensionResp> dimensionRespList = dimensionResps.stream()
                    .filter(d -> d.getModelBizName().equalsIgnoreCase(modelResp.getBizName()))
                    .collect(Collectors.toList());
            dimensionYamlMap.get(modelResp.getBizName()).addAll(DimensionYamlManager.convert2DimensionYaml(
                    DimensionConverter.dimensionInfo2Dimension(dimensionRespList)));
        }
    }

    public static Datasource datasourceInfo2Datasource(ModelResp modelResp) {
        Datasource datasource = new Datasource();
        BeanUtils.copyProperties(modelResp, datasource);
        datasource.setDatasourceDetail(modelResp.getModelDetail());
        datasource.setModelId(modelResp.getId());
        datasource.setDatabaseId(modelResp.getDatabaseId());
        return datasource;
    }

}
