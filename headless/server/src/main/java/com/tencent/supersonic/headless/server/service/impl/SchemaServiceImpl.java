package com.tencent.supersonic.headless.server.service.impl;

import com.github.pagehelper.PageInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.enums.SchemaType;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaItemQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.ViewFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ViewResp;
import com.tencent.supersonic.headless.api.pojo.response.ViewSchemaResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.service.ViewService;
import com.tencent.supersonic.headless.server.utils.DimensionConverter;
import com.tencent.supersonic.headless.server.utils.MetricConverter;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

@Slf4j
@Service
public class SchemaServiceImpl implements SchemaService {

    protected final Cache<String, List<ItemUseResp>> itemUseCache =
            CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
    private final StatUtils statUtils;
    private final ModelService modelService;
    private final DimensionService dimensionService;
    private final MetricService metricService;
    private final DomainService domainService;
    private final ViewService viewService;
    private final ModelRelaService modelRelaService;

    public SchemaServiceImpl(ModelService modelService,
                             DimensionService dimensionService,
                             MetricService metricService,
                             DomainService domainService,
                             ViewService viewService,
                             ModelRelaService modelRelaService,
                             StatUtils statUtils) {
        this.modelService = modelService;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.domainService = domainService;
        this.viewService = viewService;
        this.modelRelaService = modelRelaService;
        this.statUtils = statUtils;
    }

    @SneakyThrows
    @Override
    public List<ViewSchemaResp> fetchViewSchema(ViewFilterReq filter) {
        List<ViewSchemaResp> viewSchemaResps = new ArrayList<>();
        List<Long> viewIds = filter.getViewIds();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setStatus(StatusEnum.ONLINE.getCode());
        metaFilter.setIds(viewIds);
        List<ViewResp> viewResps = viewService.getViewList(metaFilter);
        List<Long> modelIds = viewResps.stream().map(ViewResp::getAllModels)
                .flatMap(Collection::stream).collect(Collectors.toList());
        metaFilter.setModelIds(modelIds);
        metaFilter.setIds(Lists.newArrayList());
        List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
        List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
        metaFilter.setIds(modelIds);
        List<ModelResp> modelResps = modelService.getModelList(metaFilter);
        Map<Long, ViewResp> viewRespMap = getViewMap(viewResps);
        for (Long viewId : viewRespMap.keySet()) {
            ViewResp viewResp = viewRespMap.get(viewId);
            if (viewResp == null || !StatusEnum.ONLINE.getCode().equals(viewResp.getStatus())) {
                continue;
            }
            List<MetricSchemaResp> metricSchemaResps = MetricConverter.filterByView(metricResps, viewResp)
                    .stream().map(this::convert).collect(Collectors.toList());
            List<DimSchemaResp> dimSchemaResps = DimensionConverter.filterByView(dimensionResps, viewResp)
                    .stream().map(this::convert).collect(Collectors.toList());
            ViewSchemaResp viewSchemaResp = new ViewSchemaResp();
            BeanUtils.copyProperties(viewResp, viewSchemaResp);
            viewSchemaResp.setDimensions(dimSchemaResps);
            viewSchemaResp.setMetrics(metricSchemaResps);
            viewSchemaResp.setModelResps(modelResps.stream().filter(modelResp ->
                    viewResp.getAllModels().contains(modelResp.getId())).collect(Collectors.toList()));
            viewSchemaResps.add(viewSchemaResp);
        }
        fillStaticInfo(viewSchemaResps);
        return viewSchemaResps;
    }

    public ViewSchemaResp fetchViewSchema(Long viewId) {
        if (viewId == null) {
            return null;
        }
        return fetchViewSchema(new ViewFilterReq(viewId)).stream().findFirst().orElse(null);
    }

    public List<ModelSchemaResp> fetchModelSchemaResps(List<Long> modelIds) {
        List<ModelSchemaResp> modelSchemaResps = Lists.newArrayList();
        if (CollectionUtils.isEmpty(modelIds)) {
            return modelSchemaResps;
        }
        MetaFilter metaFilter = new MetaFilter(modelIds);
        metaFilter.setStatus(StatusEnum.ONLINE.getCode());
        Map<Long, List<MetricResp>> metricRespMap = metricService.getMetrics(metaFilter)
                .stream().collect(Collectors.groupingBy(MetricResp::getModelId));
        Map<Long, List<DimensionResp>> dimensionRespsMap = dimensionService.getDimensions(metaFilter)
                .stream().collect(Collectors.groupingBy(DimensionResp::getModelId));
        List<ModelRela> modelRelas = modelRelaService.getModelRela(modelIds);
        Map<Long, ModelResp> modelMap = modelService.getModelMap();
        for (Long modelId : modelIds) {
            ModelResp modelResp = modelMap.get(modelId);
            if (modelResp == null || !StatusEnum.ONLINE.getCode().equals(modelResp.getStatus())) {
                continue;
            }
            List<MetricResp> metricResps = metricRespMap.getOrDefault(modelId, Lists.newArrayList());
            List<MetricSchemaResp> metricSchemaResps = metricResps.stream()
                    .map(this::convert).collect(Collectors.toList());
            List<DimSchemaResp> dimensionResps = dimensionRespsMap.getOrDefault(modelId, Lists.newArrayList())
                    .stream().map(this::convert).collect(Collectors.toList());
            ModelSchemaResp modelSchemaResp = new ModelSchemaResp();
            BeanUtils.copyProperties(modelResp, modelSchemaResp);
            modelSchemaResp.setDimensions(dimensionResps);
            modelSchemaResp.setMetrics(metricSchemaResps);
            modelSchemaResp.setModelRelas(modelRelas.stream().filter(modelRela
                            -> modelRela.getFromModelId().equals(modelId) || modelRela.getToModelId().equals(modelId))
                    .collect(Collectors.toList()));
            modelSchemaResps.add(modelSchemaResp);
        }
        return modelSchemaResps;

    }

    private void fillCnt(List<ViewSchemaResp> viewSchemaResps, List<ItemUseResp> statInfos) {

        Map<String, ItemUseResp> typeIdAndStatPair = statInfos.stream()
                .collect(Collectors.toMap(
                        itemUseInfo -> itemUseInfo.getType() + AT_SYMBOL + AT_SYMBOL + itemUseInfo.getBizName(),
                        itemUseInfo -> itemUseInfo,
                        (item1, item2) -> item1));
        log.debug("typeIdAndStatPair:{}", typeIdAndStatPair);
        for (ViewSchemaResp viewSchemaResp : viewSchemaResps) {
            fillDimCnt(viewSchemaResp, typeIdAndStatPair);
            fillMetricCnt(viewSchemaResp, typeIdAndStatPair);
        }
    }

    private void fillMetricCnt(ViewSchemaResp viewSchemaResp, Map<String, ItemUseResp> typeIdAndStatPair) {
        List<MetricSchemaResp> metrics = viewSchemaResp.getMetrics();
        if (CollectionUtils.isEmpty(viewSchemaResp.getMetrics())) {
            return;
        }

        if (!CollectionUtils.isEmpty(metrics)) {
            metrics.stream().forEach(metric -> {
                String key = TypeEnums.METRIC.name().toLowerCase()
                        + AT_SYMBOL + AT_SYMBOL + metric.getBizName();
                if (typeIdAndStatPair.containsKey(key)) {
                    metric.setUseCnt(typeIdAndStatPair.get(key).getUseCnt());
                }
            });
        }
        viewSchemaResp.setMetrics(metrics);
    }

    private void fillDimCnt(ViewSchemaResp viewSchemaResp, Map<String, ItemUseResp> typeIdAndStatPair) {
        List<DimSchemaResp> dimensions = viewSchemaResp.getDimensions();
        if (CollectionUtils.isEmpty(viewSchemaResp.getDimensions())) {
            return;
        }
        if (!CollectionUtils.isEmpty(dimensions)) {
            dimensions.stream().forEach(dim -> {
                String key = TypeEnums.DIMENSION.name().toLowerCase()
                        + AT_SYMBOL + AT_SYMBOL + dim.getBizName();
                if (typeIdAndStatPair.containsKey(key)) {
                    dim.setUseCnt(typeIdAndStatPair.get(key).getUseCnt());
                }
            });
        }
        viewSchemaResp.setDimensions(dimensions);
    }

    @Override
    public PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionCmd, User user) {
        return dimensionService.queryDimension(pageDimensionCmd);
    }

    @Override
    public PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user) {
        return metricService.queryMetric(pageMetricReq, user);
    }

    @Override
    public List querySchemaItem(SchemaItemQueryReq schemaItemQueryReq) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setIds(schemaItemQueryReq.getIds());
        if (TypeEnums.METRIC.equals(schemaItemQueryReq.getType())) {
            return metricService.getMetrics(metaFilter);
        } else if (TypeEnums.DIMENSION.equals(schemaItemQueryReq.getType())) {
            return dimensionService.getDimensions(metaFilter);
        }
        throw new InvalidArgumentException("暂不支持的类型" + schemaItemQueryReq.getType().name());
    }

    @Override
    public List<DomainResp> getDomainList(User user) {
        return domainService.getDomainListWithAdminAuth(user);
    }

    @Override
    public List<ModelResp> getModelList(User user, AuthType authTypeEnum, Long domainId) {
        return modelService.getModelListWithAuth(user, domainId, authTypeEnum);
    }

    @Override
    public List<ViewResp> getViewList(Long domainId) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setDomainId(domainId);
        return viewService.getViewList(metaFilter);
    }

    @Override
    public SemanticSchemaResp fetchSemanticSchema(SchemaFilterReq schemaFilterReq) {
        SemanticSchemaResp semanticSchemaResp = new SemanticSchemaResp();
        semanticSchemaResp.setViewId(schemaFilterReq.getViewId());
        semanticSchemaResp.setModelIds(schemaFilterReq.getModelIds());
        if (schemaFilterReq.getViewId() != null) {
            ViewSchemaResp viewSchemaResp = fetchViewSchema(schemaFilterReq.getViewId());
            BeanUtils.copyProperties(viewSchemaResp, semanticSchemaResp);
            List<Long> modelIds = viewSchemaResp.getAllModels();
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setIds(modelIds);
            List<ModelResp> modelList = modelService.getModelList(metaFilter);
            metaFilter.setModelIds(modelIds);
            List<ModelRela> modelRelas = modelRelaService.getModelRela(modelIds);
            semanticSchemaResp.setModelResps(modelList);
            semanticSchemaResp.setModelRelas(modelRelas);
            semanticSchemaResp.setModelIds(modelIds);
            semanticSchemaResp.setSchemaType(SchemaType.VIEW);
        } else if (!CollectionUtils.isEmpty(schemaFilterReq.getModelIds())) {
            List<ModelSchemaResp> modelSchemaResps = fetchModelSchemaResps(schemaFilterReq.getModelIds());
            semanticSchemaResp.setMetrics(modelSchemaResps.stream().map(ModelSchemaResp::getMetrics)
                    .flatMap(Collection::stream).collect(Collectors.toList()));
            semanticSchemaResp.setDimensions(modelSchemaResps.stream().map(ModelSchemaResp::getDimensions)
                    .flatMap(Collection::stream).collect(Collectors.toList()));
            semanticSchemaResp.setModelRelas(modelSchemaResps.stream().map(ModelSchemaResp::getModelRelas)
                    .flatMap(Collection::stream).collect(Collectors.toList()));
            semanticSchemaResp.setModelResps(modelSchemaResps.stream().map(this::convert).collect(Collectors.toList()));
            semanticSchemaResp.setSchemaType(SchemaType.MODEL);
        }
        if (!CollectionUtils.isEmpty(semanticSchemaResp.getModelIds())) {
            DatabaseResp databaseResp = modelService.getDatabaseByModelId(semanticSchemaResp.getModelIds().get(0));
            semanticSchemaResp.setDatabaseResp(databaseResp);
        }
        return semanticSchemaResp;
    }

    @SneakyThrows
    @Override
    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) {
        if (itemUseReq.getCacheEnable()) {
            return itemUseCache.get(JsonUtil.toString(itemUseReq), () -> {
                List<ItemUseResp> data = statUtils.getStatInfo(itemUseReq);
                itemUseCache.put(JsonUtil.toString(itemUseReq), data);
                return data;
            });
        }
        return statUtils.getStatInfo(itemUseReq);
    }

    @Override
    public List<ItemResp> getDomainViewTree() {
        List<DomainResp> domainResps = domainService.getDomainList();
        List<ItemResp> itemResps = domainResps.stream().map(domain ->
                        new ItemResp(domain.getId(), domain.getParentId(), domain.getName(), TypeEnums.DOMAIN))
                .collect(Collectors.toList());
        Map<Long, ItemResp> itemRespMap = itemResps.stream()
                .collect(Collectors.toMap(ItemResp::getId, item -> item));
        for (ItemResp itemResp : itemResps) {
            ItemResp parentItem = itemRespMap.get(itemResp.getParentId());
            if (parentItem == null) {
                continue;
            }
            parentItem.getChildren().add(itemResp);
        }
        List<ViewResp> viewResps = viewService.getViewList(new MetaFilter());
        for (ViewResp viewResp : viewResps) {
            ItemResp itemResp = itemRespMap.get(viewResp.getDomainId());
            if (itemResp != null) {
                ItemResp view = new ItemResp(viewResp.getId(), viewResp.getDomainId(),
                        viewResp.getName(), TypeEnums.VIEW);
                itemResp.getChildren().add(view);
            }
        }
        return itemResps.stream().filter(itemResp -> itemResp.getParentId() == 0)
                .collect(Collectors.toList());
    }

    private void fillStaticInfo(List<ViewSchemaResp> viewSchemaResps) {
        List<Long> viewIds = viewSchemaResps.stream()
                .map(ViewSchemaResp::getId).collect(Collectors.toList());
        ItemUseReq itemUseReq = new ItemUseReq();
        itemUseReq.setModelIds(viewIds);

        List<ItemUseResp> statInfos = getStatInfo(itemUseReq);
        log.debug("statInfos:{}", statInfos);
        fillCnt(viewSchemaResps, statInfos);
    }

    private Map<Long, ViewResp> getViewMap(List<ViewResp> viewResps) {
        if (CollectionUtils.isEmpty(viewResps)) {
            return new HashMap<>();
        }
        return viewResps.stream().collect(
                Collectors.toMap(ViewResp::getId, viewResp -> viewResp));
    }

    private DimSchemaResp convert(DimensionResp dimensionResp) {
        DimSchemaResp dimSchemaResp = new DimSchemaResp();
        BeanUtils.copyProperties(dimensionResp, dimSchemaResp);
        return dimSchemaResp;
    }

    private MetricSchemaResp convert(MetricResp metricResp) {
        MetricSchemaResp metricSchemaResp = new MetricSchemaResp();
        BeanUtils.copyProperties(metricResp, metricSchemaResp);
        return metricSchemaResp;
    }

    private ModelResp convert(ModelSchemaResp modelSchemaResp) {
        ModelResp modelResp = new ModelResp();
        BeanUtils.copyProperties(modelSchemaResp, modelResp);
        return modelResp;
    }

}
