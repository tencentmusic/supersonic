package com.tencent.supersonic.semantic.model.application;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.pojo.RelateDimension;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import com.tencent.supersonic.semantic.model.domain.pojo.Model;
import com.tencent.supersonic.semantic.model.domain.repository.ModelRepository;
import com.tencent.supersonic.semantic.model.domain.utils.ModelConvert;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;
    private final MetricService metricService;
    private final DimensionService dimensionService;
    private final DatasourceService datasourceService;
    private final DomainService domainService;
    private final UserService userService;
    private final DatabaseService databaseService;

    private final Catalog catalog;

    public ModelServiceImpl(ModelRepository modelRepository, @Lazy MetricService metricService,
            @Lazy DimensionService dimensionService, @Lazy DatasourceService datasourceService,
            @Lazy DomainService domainService, UserService userService,
            @Lazy DatabaseService databaseService,
            @Lazy Catalog catalog) {
        this.modelRepository = modelRepository;
        this.metricService = metricService;
        this.dimensionService = dimensionService;
        this.datasourceService = datasourceService;
        this.domainService = domainService;
        this.userService = userService;
        this.databaseService = databaseService;
        this.catalog = catalog;
    }

    @Override
    public void createModel(ModelReq modelReq, User user) {
        log.info("[create model] req : {}", JSONObject.toJSONString(modelReq));
        Model model = ModelConvert.convert(modelReq);
        log.info("[create model] object:{}", JSONObject.toJSONString(modelReq));
        saveModel(model, user);
    }

    @Override
    public void updateModel(ModelReq modelReq, User user) {
        ModelDO modelDO = getModelDO(modelReq.getId());
        modelDO.setUpdatedAt(new Date());
        modelDO.setUpdatedBy(user.getName());
        BeanMapper.mapper(modelReq, modelDO);
        modelDO.setAdmin(String.join(",", modelReq.getAdmins()));
        modelDO.setAdminOrg(String.join(",", modelReq.getAdminOrgs()));
        modelDO.setViewer(String.join(",", modelReq.getViewers()));
        modelDO.setViewOrg(String.join(",", modelReq.getViewOrgs()));
        modelDO.setEntity(JsonUtil.toString(modelReq.getEntity()));
        modelRepository.updateModel(modelDO);
    }

    @Override
    public void deleteModel(Long id) {
        checkDelete(id);
        modelRepository.deleteModel(id);
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
        return fillMetricInfo(new ArrayList<>(modelRespSet));
    }

    public List<ModelResp> getModelRespAuthInheritDomain(User user, AuthType authType) {
        Set<DomainResp> domainResps = domainService.getDomainAuthSet(user, authType);
        if (CollectionUtils.isEmpty(domainResps)) {
            return Lists.newArrayList();
        }
        List<ModelResp> allModelList = getModelList();
        Set<Long> domainIds = domainResps.stream().map(DomainResp::getId).collect(Collectors.toSet());
        return allModelList.stream().filter(modelResp ->
                domainIds.contains(modelResp.getDomainId())).collect(Collectors.toList());
    }

    @Override
    public List<ModelResp> getModelAuthList(User user, AuthType authTypeEnum) {
        List<ModelResp> modelResps = getModelList();
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
        List<ModelResp> modelResps = getModelList();
        if (CollectionUtils.isEmpty(modelResps)) {
            return modelResps;
        }
        return modelResps.stream().filter(modelResp ->
                domainIds.contains(modelResp.getDomainId())).collect(Collectors.toList());
    }


    @Override
    public List<ModelResp> getModelList(List<Long> modelIds) {
        return getModelList().stream()
                .filter(modelDO -> modelIds.contains(modelDO.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelResp> getModelList() {
        return convertList(modelRepository.getModelList());
    }

    @Override
    public ModelResp getModel(Long id) {
        Map<Long, DomainResp> domainRespMap = domainService.getDomainList().stream()
                .collect(Collectors.toMap(DomainResp::getId, d -> d));
        return ModelConvert.convert(getModelDO(id), domainRespMap);
    }

    private void checkDelete(Long id) {
        List<MetricResp> metricResps = metricService.getMetrics(id);
        List<DimensionResp> dimensionResps = dimensionService.getDimensions(id);
        List<DatasourceResp> datasourceResps = datasourceService.getDatasourceList(id);
        if (!CollectionUtils.isEmpty(metricResps) || !CollectionUtils.isEmpty(datasourceResps)
                || !CollectionUtils.isEmpty(dimensionResps)) {
            throw new RuntimeException("该模型下存在数据源、指标或者维度, 暂不能删除, 请确认");
        }
    }

    private void saveModel(Model model, User user) {
        ModelDO modelDO = ModelConvert.convert(model, user);
        modelRepository.createModel(modelDO);
        model.setId(modelDO.getId());
    }

    private List<ModelResp> convertList(List<ModelDO> modelDOS) {
        List<ModelResp> modelResps = Lists.newArrayList();
        if (CollectionUtils.isEmpty(modelDOS)) {
            return modelResps;
        }
        Map<Long, DomainResp> domainRespMap = domainService.getDomainList()
                .stream().collect(Collectors.toMap(DomainResp::getId, d -> d));
        return modelDOS.stream()
                .map(modelDO -> ModelConvert.convert(modelDO, domainRespMap))
                .collect(Collectors.toList());
    }

    private List<ModelResp> fillMetricInfo(List<ModelResp> modelResps) {
        if (CollectionUtils.isEmpty(modelResps)) {
            return modelResps;
        }
        Map<Long, List<MetricResp>> metricMap = metricService.getMetrics().stream()
                .collect(Collectors.groupingBy(MetricResp::getModelId));
        Map<Long, List<DimensionResp>> dimensionMap = dimensionService.getDimensions().stream()
                .collect(Collectors.groupingBy(DimensionResp::getModelId));
        modelResps.forEach(modelResp -> {
            modelResp.setDimensionCnt(dimensionMap.getOrDefault(modelResp.getId(), Lists.newArrayList()).size());
            modelResp.setMetricCnt(metricMap.getOrDefault(modelResp.getId(), Lists.newArrayList()).size());
        });
        return modelResps;
    }


    @Override
    public Map<Long, ModelResp> getModelMap() {
        return getModelList().stream().collect(Collectors.toMap(ModelResp::getId, a -> a, (k1, k2) -> k1));
    }

    @Override
    public Map<Long, String> getModelFullPathMap() {
        return getModelList().stream().filter(m -> m != null && m.getFullPath() != null)
                .collect(Collectors.toMap(ModelResp::getId,
                        ModelResp::getFullPath, (k1, k2) -> k1));
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

    protected ModelDO getModelDO(Long id) {
        return modelRepository.getModelById(id);
    }

    private ModelSchemaResp fetchSingleModelSchema(ModelResp modelResp) {
        Long modelId = modelResp.getId();
        ModelSchemaResp modelSchemaResp = new ModelSchemaResp();
        BeanUtils.copyProperties(modelResp, modelSchemaResp);
        modelSchemaResp.setDimensions(generateDimSchema(modelId));
        modelSchemaResp.setMetrics(generateMetricSchema(modelId, modelResp));
        return modelSchemaResp;
    }

    @Override
    public ModelSchemaResp fetchSingleModelSchema(Long modelId) {
        ModelResp model = getModel(modelId);
        return fetchSingleModelSchema(model);
    }

    @Override
    public List<ModelSchemaResp> fetchModelSchema(ModelSchemaFilterReq modelSchemaFilterReq) {
        List<ModelSchemaResp> modelSchemaRespList = new ArrayList<>();
        List<Long> modelIds = modelSchemaFilterReq.getModelIds();
        if (CollectionUtils.isEmpty(modelIds)) {
            modelIds = generateModelIdsReq(modelSchemaFilterReq);
        }
        Map<Long, List<MetricResp>> metricRespMap = metricService.getMetricsByModelIds(modelIds)
                .stream().collect(Collectors.groupingBy(MetricResp::getModelId));
        Map<Long, List<DimensionResp>> dimensionRespsMap = dimensionService.getDimensionsByModelIds(modelIds)
                .stream().collect(Collectors.groupingBy(DimensionResp::getModelId));
        Map<Long, List<MeasureResp>> measureRespsMap = datasourceService.getMeasureListOfModel(modelIds)
                .stream().collect(Collectors.groupingBy(MeasureResp::getModelId));
        for (Long modelId : modelIds) {
            ModelResp modelResp = getModelMap().get(modelId);
            if (modelResp == null) {
                continue;
            }
            List<MeasureResp> measureResps = measureRespsMap.getOrDefault(modelId, Lists.newArrayList());
            List<MetricResp> metricResps = metricRespMap.getOrDefault(modelId, Lists.newArrayList());
            List<MetricSchemaResp> metricSchemaResps = metricResps.stream().map(metricResp ->
                    convert(metricResp, metricResps, measureResps, modelResp)).collect(Collectors.toList());
            List<DimSchemaResp> dimensionResps = dimensionRespsMap.getOrDefault(modelId, Lists.newArrayList())
                    .stream().map(this::convert).collect(Collectors.toList());
            ModelSchemaResp modelSchemaResp = new ModelSchemaResp();
            BeanUtils.copyProperties(modelResp, modelSchemaResp);
            modelSchemaResp.setDimensions(dimensionResps);
            modelSchemaResp.setMetrics(metricSchemaResps);
            modelSchemaRespList.add(modelSchemaResp);
        }
        return modelSchemaRespList;
    }

    @Override
    public DatabaseResp getDatabaseByModelId(Long modelId) {
        List<DatasourceResp> datasourceResps = datasourceService.getDatasourceList(modelId);
        if (!CollectionUtils.isEmpty(datasourceResps)) {
            Long databaseId = datasourceResps.iterator().next().getDatabaseId();
            return databaseService.getDatabase(databaseId);
        }
        return null;
    }

    private List<MetricSchemaResp> generateMetricSchema(Long modelId, ModelResp modelResp) {
        List<MetricSchemaResp> metricSchemaDescList = new ArrayList<>();
        List<MetricResp> metricResps = metricService.getMetrics(modelId);
        List<MeasureResp> measureResps = datasourceService.getMeasureListOfModel(modelId);
        metricResps.stream().forEach(metricResp ->
                metricSchemaDescList.add(convert(metricResp, metricResps, measureResps, modelResp)));
        return metricSchemaDescList;
    }

    private List<DimSchemaResp> generateDimSchema(Long modelId) {
        List<DimensionResp> dimDescList = dimensionService.getDimensions(modelId);
        return dimDescList.stream().map(this::convert).collect(Collectors.toList());
    }

    private DimSchemaResp convert(DimensionResp dimensionResp) {
        DimSchemaResp dimSchemaResp = new DimSchemaResp();
        BeanUtils.copyProperties(dimensionResp, dimSchemaResp);
        dimSchemaResp.setUseCnt(0L);
        return dimSchemaResp;
    }

    private MetricSchemaResp convert(MetricResp metricResp, List<MetricResp> metricResps,
                                     List<MeasureResp> measureResps, ModelResp modelResp) {
        MetricSchemaResp metricSchemaResp = new MetricSchemaResp();
        BeanUtils.copyProperties(metricResp, metricSchemaResp);
        RelateDimension relateDimension = metricResp.getRelateDimension();
        if (relateDimension == null || CollectionUtils.isEmpty(relateDimension.getDrillDownDimensions())) {
            metricSchemaResp.setRelateDimension(RelateDimension.builder()
                    .drillDownDimensions(modelResp.getDrillDownDimensions()).build());
        }
        metricSchemaResp.setUseCnt(0L);
        String agg = catalog.getAgg(metricResps, measureResps, metricSchemaResp.getBizName());
        metricSchemaResp.setDefaultAgg(agg);
        return metricSchemaResp;
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
        List<String> admins = modelResp.getAdmins();
        List<String> viewers = modelResp.getViewers();
        List<String> adminOrgs = modelResp.getAdminOrgs();
        List<String> viewOrgs = modelResp.getViewOrgs();
        if (user.isSuperAdmin()) {
            return true;
        }
        if (modelResp.openToAll()) {
            return true;
        }
        String userName = user.getName();
        if (admins.contains(userName) || viewers.contains(userName) || modelResp.getCreatedBy().equals(userName)) {
            return true;
        }
        if (CollectionUtils.isEmpty(adminOrgs) && CollectionUtils.isEmpty(viewOrgs)) {
            return false;
        }
        for (String orgId : orgIds) {
            if (adminOrgs.contains(orgId)) {
                return true;
            }
        }
        for (String orgId : orgIds) {
            if (viewOrgs.contains(orgId)) {
                return true;
            }
        }
        return false;
    }
}
