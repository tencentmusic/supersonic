package com.tencent.supersonic.headless.server.web.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.request.DateInfoReq;
import com.tencent.supersonic.headless.api.pojo.request.DimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.FieldRemovedReq;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.UnAvailableItemResp;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DateInfoDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelDO;
import com.tencent.supersonic.headless.server.persistence.repository.DateInfoRepository;
import com.tencent.supersonic.headless.server.persistence.repository.ModelRepository;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import com.tencent.supersonic.headless.server.web.service.DatabaseService;
import com.tencent.supersonic.headless.server.web.service.DimensionService;
import com.tencent.supersonic.headless.server.web.service.DomainService;
import com.tencent.supersonic.headless.server.web.service.MetricService;
import com.tencent.supersonic.headless.server.web.service.ModelService;
import com.tencent.supersonic.headless.server.web.service.DataSetService;
import com.tencent.supersonic.headless.server.utils.ModelConverter;
import com.tencent.supersonic.headless.server.utils.NameCheckUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    private UserService userService;

    private DataSetService dataSetService;

    private DateInfoRepository dateInfoRepository;

    public ModelServiceImpl(ModelRepository modelRepository,
                            DatabaseService databaseService,
                            @Lazy DimensionService dimensionService,
                            @Lazy MetricService metricService,
                            DomainService domainService,
                            UserService userService,
                            DataSetService dataSetService,
                            DateInfoRepository dateInfoRepository) {
        this.modelRepository = modelRepository;
        this.databaseService = databaseService;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.domainService = domainService;
        this.userService = userService;
        this.dataSetService = dataSetService;
        this.dateInfoRepository = dateInfoRepository;
    }

    @Override
    @Transactional
    public ModelResp createModel(ModelReq modelReq, User user) throws Exception {
        checkName(modelReq);
        ModelDO modelDO = ModelConverter.convert(modelReq, user);
        modelRepository.createModel(modelDO);
        batchCreateDimension(modelDO, user);
        batchCreateMetric(modelDO, user);
        return ModelConverter.convert(modelDO);
    }

    @Override
    @Transactional
    public ModelResp updateModel(ModelReq modelReq, User user) throws Exception {
        checkName(modelReq);
        ModelDO modelDO = modelRepository.getModelById(modelReq.getId());
        ModelConverter.convert(modelDO, modelReq, user);
        modelRepository.updateModel(modelDO);
        batchCreateDimension(modelDO, user);
        batchCreateMetric(modelDO, user);
        return ModelConverter.convert(modelDO);
    }

    @Override
    public List<ModelResp> getModelList(MetaFilter metaFilter) {
        ModelFilter modelFilter = new ModelFilter();
        BeanUtils.copyProperties(metaFilter, modelFilter);
        List<ModelResp> modelResps = ModelConverter.convertList(modelRepository.getModelList(modelFilter));
        if (modelFilter.getDataSetId() != null) {
            DataSetResp dataSetResp = dataSetService.getDataSet(modelFilter.getDataSetId());
            return modelResps.stream().filter(modelResp -> dataSetResp.getAllModels().contains(modelResp.getId()))
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
    public UnAvailableItemResp getUnAvailableItem(FieldRemovedReq fieldRemovedReq) {
        if (CollectionUtils.isEmpty(fieldRemovedReq.getFields())) {
            return UnAvailableItemResp.builder().build();
        }
        MetaFilter metaFilter = new MetaFilter(Lists.newArrayList(fieldRemovedReq.getModelId()));
        metaFilter.setFieldsDepend(fieldRemovedReq.getFields());
        List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
        List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
        return UnAvailableItemResp.builder().dimensionResps(dimensionResps)
                .metricResps(metricResps).build();
    }

    private void batchCreateDimension(ModelDO modelDO, User user) throws Exception {
        List<DimensionReq> dimensionReqs = ModelConverter.convertDimensionList(modelDO);
        dimensionService.createDimensionBatch(dimensionReqs, user);
    }

    private void batchCreateMetric(ModelDO datasourceDO, User user) throws Exception {
        List<MetricReq> metricReqs = ModelConverter.convertMetricList(datasourceDO);
        metricService.createMetricBatch(metricReqs, user);
    }

    private void checkName(ModelReq modelReq) {
        String forbiddenCharacters = NameCheckUtils.findForbiddenCharacters(modelReq.getName());
        if (StringUtils.isNotBlank(forbiddenCharacters)) {
            String message = String.format("模型名称[%s]包含特殊字符(%s), 请修改", modelReq.getName(), forbiddenCharacters);
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
            String measureForbiddenCharacters = NameCheckUtils.findForbiddenCharacters(measure.getName());
            if (StringUtils.isNotBlank(measure.getName())
                    && StringUtils.isNotBlank(measureForbiddenCharacters)) {
                String message = String.format("度量[%s]包含特殊字符(%s), 请修改", measure.getName(), measureForbiddenCharacters);
                throw new InvalidArgumentException(message);
            }
        }
        for (Dim dim : dims) {
            String dimForbiddenCharacters = NameCheckUtils.findForbiddenCharacters(dim.getName());
            if (StringUtils.isNotBlank(dim.getName())
                    && StringUtils.isNotBlank(dimForbiddenCharacters)) {
                String message = String.format("维度[%s]包含特殊字符(%s), 请修改", dim.getName(), dimForbiddenCharacters);
                throw new InvalidArgumentException(message);
            }
        }
        for (Identify identify : identifies) {
            String identifyForbiddenCharacters = NameCheckUtils.findForbiddenCharacters(identify.getName());
            if (StringUtils.isNotBlank(identify.getName())
                    && StringUtils.isNotBlank(identifyForbiddenCharacters)) {
                String message = String.format("主键/外键[%s]包含特殊字符(%s), 请修改", identify.getName(),
                        identifyForbiddenCharacters);
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
        List<ModelResp> modelRespsAuthInheritDomain = getModelRespAuthInheritDomain(user, domainId, authType);
        modelRespSet.addAll(modelRespsAuthInheritDomain);
        return modelRespSet.stream().sorted(Comparator.comparingLong(ModelResp::getId))
                .collect(Collectors.toList());
    }

    public List<ModelResp> getModelRespAuthInheritDomain(User user, Long domainId, AuthType authType) {
        List<Long> domainIds = domainService.getDomainAuthSet(user, authType)
                .stream().filter(domainResp -> {
                    if (domainId == null) {
                        return true;
                    } else {
                        return domainId.equals(domainResp.getId()) || domainId.equals(domainResp.getParentId());
                    }
                }).map(DomainResp::getId)
                .collect(Collectors.toList());
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
        if (authTypeEnum.equals(AuthType.VISIBLE)) {
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
        modelFilter.setIncludesDetail(false);
        List<ModelResp> modelResps = getModelList(modelFilter);
        if (CollectionUtils.isEmpty(modelResps)) {
            return modelResps;
        }
        return modelResps.stream().filter(modelResp ->
                domainIds.contains(modelResp.getDomainId())).collect(Collectors.toList());
    }

    @Override
    public List<ModelResp> getAllModelByDomainIds(List<Long> domainIds) {
        Set<DomainResp> domainResps = domainService.getDomainChildren(domainIds);
        List<Long> allDomainIds = domainResps.stream().map(DomainResp::getId).collect(Collectors.toList());
        return getModelByDomainIds(allDomainIds);
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
                    if (StatusEnum.OFFLINE.getCode().equals(metaBatchReq.getStatus())
                            || StatusEnum.DELETED.getCode().equals(metaBatchReq.getStatus())) {
                        metricService.sendMetricEventBatch(Lists.newArrayList(modelDO.getId()), EventType.DELETE);
                        dimensionService.sendDimensionEventBatch(Lists.newArrayList(modelDO.getId()), EventType.DELETE);
                    } else if (StatusEnum.ONLINE.getCode().equals(metaBatchReq.getStatus())) {
                        metricService.sendMetricEventBatch(Lists.newArrayList(modelDO.getId()), EventType.ADD);
                        dimensionService.sendDimensionEventBatch(Lists.newArrayList(modelDO.getId()), EventType.ADD);
                    }
                })
                .collect(Collectors.toList());
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
            dateInfoCommend.setUnavailableDateList(JsonUtil.toList(dateInfoDO.getUnavailableDateList(), String.class));
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

    public static boolean checkDataSetPermission(Set<String> orgIds, User user, ModelResp modelResp) {
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

}
