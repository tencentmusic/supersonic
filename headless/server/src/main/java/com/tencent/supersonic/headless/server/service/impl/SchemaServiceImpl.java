package com.tencent.supersonic.headless.server.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.enums.SchemaType;
import com.tencent.supersonic.headless.api.pojo.request.DataSetFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.*;
import com.tencent.supersonic.headless.server.manager.DimensionYamlManager;
import com.tencent.supersonic.headless.server.manager.MetricYamlManager;
import com.tencent.supersonic.headless.server.manager.ModelYamlManager;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.headless.server.service.*;
import com.tencent.supersonic.headless.server.utils.DataSetSchemaBuilder;
import com.tencent.supersonic.headless.server.utils.DimensionConverter;
import com.tencent.supersonic.headless.server.utils.MetricConverter;
import com.tencent.supersonic.headless.server.utils.StatUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.AT_SYMBOL;

@Slf4j
@Service
public class SchemaServiceImpl implements SchemaService {

    protected final Cache<String, List<ItemUseResp>> itemUseCache =
            CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    protected final Cache<DataSetFilterReq, List<DataSetSchemaResp>> dataSetSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    protected final Cache<SchemaFilterReq, SemanticSchemaResp> semanticSchemaCache =
            CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    private final StatUtils statUtils;
    private final ModelService modelService;
    private final DimensionService dimensionService;
    private final MetricService metricService;
    private final DomainService domainService;
    private final DataSetService dataSetService;
    private final ModelRelaService modelRelaService;
    private final TermService termService;
    private final DatabaseService databaseService;

    @Value("${s2.schema.cache.enable:true}")
    private boolean schemaCacheEnable;

    public SchemaServiceImpl(ModelService modelService, DimensionService dimensionService,
            MetricService metricService, DomainService domainService, DataSetService dataSetService,
            ModelRelaService modelRelaService, StatUtils statUtils, TermService termService,
            DatabaseService databaseService) {
        this.modelService = modelService;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.domainService = domainService;
        this.dataSetService = dataSetService;
        this.modelRelaService = modelRelaService;
        this.statUtils = statUtils;
        this.termService = termService;
        this.databaseService = databaseService;
    }

    public List<DataSetSchemaResp> fetchDataSetSchema(DataSetFilterReq filter) {
        List<DataSetSchemaResp> dataSetList = Lists.newArrayList();
        if (schemaCacheEnable) {
            dataSetList = dataSetSchemaCache.getIfPresent(filter);
        }
        if (CollectionUtils.isEmpty(dataSetList)) {
            dataSetList = buildDataSetSchema(filter);
            dataSetSchemaCache.put(filter, dataSetList);
        }
        return dataSetList;
    }

    public DataSetSchemaResp fetchDataSetSchema(Long dataSetId) {
        if (dataSetId == null) {
            return null;
        }
        return fetchDataSetSchema(new DataSetFilterReq(dataSetId)).stream().findFirst()
                .orElse(null);
    }

    private List<DataSetSchemaResp> fetchDataSetSchema(Set<Long> ids) {
        DataSetFilterReq dataSetFilterReq = new DataSetFilterReq();
        dataSetFilterReq.setDataSetIds(new ArrayList(ids));
        return fetchDataSetSchema(dataSetFilterReq);
    }

    @Override
    public DataSetSchema getDataSetSchema(Long dataSetId) {
        Set<Long> ids = Sets.newHashSet();
        ids.add(dataSetId);
        List<DataSetSchemaResp> dataSetSchemaResps = fetchDataSetSchema(ids);
        if (!CollectionUtils.isEmpty(dataSetSchemaResps)) {
            Optional<DataSetSchemaResp> dataSetSchemaResp = dataSetSchemaResps.stream()
                    .filter(d -> d.getId().equals(dataSetId)).findFirst();
            if (dataSetSchemaResp.isPresent()) {
                DataSetSchemaResp dataSetSchema = dataSetSchemaResp.get();
                return DataSetSchemaBuilder.build(dataSetSchema);
            }
        }
        return null;
    }

    public List<DataSetSchema> getDataSetSchema(Set<Long> ids) {
        List<DataSetSchema> domainSchemaList = new ArrayList<>();

        for (DataSetSchemaResp resp : fetchDataSetSchema(ids)) {
            domainSchemaList.add(DataSetSchemaBuilder.build(resp));
        }

        return domainSchemaList;
    }

    @Override
    public SemanticSchema getSemanticSchema() {
        return new SemanticSchema(getDataSetSchema(Collections.EMPTY_SET));
    }

    @Override
    public SemanticSchema getSemanticSchema(Set<Long> dataSetIds) {
        return new SemanticSchema(getDataSetSchema(dataSetIds));
    }

    public List<DataSetSchemaResp> buildDataSetSchema(DataSetFilterReq filter) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setStatus(StatusEnum.ONLINE.getCode());
        metaFilter.setIds(filter.getDataSetIds());
        List<DataSetResp> dataSetResps = dataSetService.getDataSetList(metaFilter);
        Map<Long, DataSetResp> dataSetRespMap = getDataSetMap(dataSetResps);
        Set<Long> domainIds =
                dataSetResps.stream().map(DataSetResp::getDomainId).collect(Collectors.toSet());
        List<Long> modelIds = dataSetRespMap.values().stream().map(DataSetResp::getAllModels)
                .flatMap(Collection::stream).collect(Collectors.toList());
        Map<Long, List<TermResp>> termMaps = termService.getTermSets(domainIds);

        metaFilter.setModelIds(modelIds);
        metaFilter.setIds(Lists.newArrayList());

        List<MetricResp> metricResps = metricService.getMetrics(metaFilter);

        List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);

        metaFilter.setIds(modelIds);
        List<ModelResp> modelResps = modelService.getModelList(metaFilter);
        metricService.batchFillMetricDefaultAgg(metricResps, modelResps);
        TagFilter tagFilter = new TagFilter();
        tagFilter.setModelIds(modelIds);

        List<DataSetSchemaResp> dataSetSchemaResps = new ArrayList<>();
        for (Long dataSetId : dataSetRespMap.keySet()) {
            DataSetResp dataSetResp = dataSetRespMap.get(dataSetId);
            if (dataSetResp == null
                    || !StatusEnum.ONLINE.getCode().equals(dataSetResp.getStatus())) {
                continue;
            }
            List<MetricSchemaResp> metricSchemaResps =
                    MetricConverter.filterByDataSet(metricResps, dataSetResp).stream()
                            .map(this::convert).collect(Collectors.toList());
            List<DimSchemaResp> dimSchemaResps =
                    DimensionConverter.filterByDataSet(dimensionResps, dataSetResp).stream()
                            .map(this::convert).collect(Collectors.toList());
            DataSetSchemaResp dataSetSchemaResp = new DataSetSchemaResp();
            BeanUtils.copyProperties(dataSetResp, dataSetSchemaResp);
            dataSetSchemaResp.setDimensions(dimSchemaResps);
            dataSetSchemaResp.setMetrics(metricSchemaResps);
            dataSetSchemaResp.setModelResps(modelResps.stream()
                    .filter(modelResp -> dataSetResp.getAllModels().contains(modelResp.getId()))
                    .collect(Collectors.toList()));
            dataSetSchemaResp.setTermResps(
                    termMaps.getOrDefault(dataSetResp.getDomainId(), Lists.newArrayList()));
            if (!CollectionUtils.isEmpty(dataSetSchemaResp.getModelResps())) {
                DatabaseResp databaseResp = databaseService
                        .getDatabase(dataSetSchemaResp.getModelResps().get(0).getDatabaseId());
                dataSetSchemaResp.setDatabaseType(databaseResp.getType());
                dataSetSchemaResp.setDatabaseVersion(databaseResp.getVersion());
            }
            dataSetSchemaResps.add(dataSetSchemaResp);
        }
        fillStaticInfo(dataSetSchemaResps);
        return dataSetSchemaResps;
    }

    public List<ModelSchemaResp> fetchModelSchemaResps(List<Long> modelIds) {
        List<ModelSchemaResp> modelSchemaResps = Lists.newArrayList();
        if (CollectionUtils.isEmpty(modelIds)) {
            return modelSchemaResps;
        }
        MetaFilter metaFilter = new MetaFilter(modelIds);
        metaFilter.setStatus(StatusEnum.ONLINE.getCode());
        Map<Long, List<MetricResp>> metricRespMap = metricService.getMetrics(metaFilter).stream()
                .collect(Collectors.groupingBy(MetricResp::getModelId));
        Map<Long, List<DimensionResp>> dimensionRespsMap =
                dimensionService.getDimensions(metaFilter).stream()
                        .collect(Collectors.groupingBy(DimensionResp::getModelId));
        List<ModelRela> modelRelas = modelRelaService.getModelRela(modelIds);
        ModelFilter modelFilter = new ModelFilter(true, modelIds);
        Map<Long, ModelResp> modelMap = modelService.getModelMap(modelFilter);
        for (Long modelId : modelIds) {
            ModelResp modelResp = modelMap.get(modelId);
            if (modelResp == null || !StatusEnum.ONLINE.getCode().equals(modelResp.getStatus())) {
                continue;
            }
            List<MetricResp> metricResps =
                    metricRespMap.getOrDefault(modelId, Lists.newArrayList());
            List<MetricSchemaResp> metricSchemaResps =
                    metricResps.stream().map(this::convert).collect(Collectors.toList());
            List<DimSchemaResp> dimensionResps =
                    dimensionRespsMap.getOrDefault(modelId, Lists.newArrayList()).stream()
                            .map(this::convert).collect(Collectors.toList());
            ModelSchemaResp modelSchemaResp = new ModelSchemaResp();
            BeanUtils.copyProperties(modelResp, modelSchemaResp);
            modelSchemaResp.setDimensions(dimensionResps);
            modelSchemaResp.setMetrics(metricSchemaResps);
            modelSchemaResp.setModelRelas(modelRelas.stream()
                    .filter(modelRela -> modelRela.getFromModelId().equals(modelId)
                            || modelRela.getToModelId().equals(modelId))
                    .collect(Collectors.toList()));
            modelSchemaResps.add(modelSchemaResp);
        }
        return modelSchemaResps;
    }

    private void fillCnt(List<DataSetSchemaResp> dataSetSchemaResps, List<ItemUseResp> statInfos) {
        if (CollectionUtils.isEmpty(statInfos)) {
            return;
        }
        Map<String, ItemUseResp> typeIdAndStatPair = statInfos.stream()
                .collect(Collectors.toMap(
                        itemUseInfo -> itemUseInfo.getType() + AT_SYMBOL + AT_SYMBOL
                                + itemUseInfo.getBizName(),
                        itemUseInfo -> itemUseInfo, (item1, item2) -> item1));
        log.debug("typeIdAndStatPair:{}", typeIdAndStatPair);
        for (DataSetSchemaResp dataSetSchemaResp : dataSetSchemaResps) {
            fillDimCnt(dataSetSchemaResp, typeIdAndStatPair);
            fillMetricCnt(dataSetSchemaResp, typeIdAndStatPair);
        }
    }

    private void fillMetricCnt(DataSetSchemaResp dataSetSchemaResp,
            Map<String, ItemUseResp> typeIdAndStatPair) {
        List<MetricSchemaResp> metrics = dataSetSchemaResp.getMetrics();
        if (CollectionUtils.isEmpty(metrics)) {
            return;
        }
        metrics.forEach(metric -> {
            metric.setUseCnt(getItemCnt(metric, typeIdAndStatPair));
        });
        dataSetSchemaResp.setMetrics(metrics);
    }

    private void fillDimCnt(DataSetSchemaResp dataSetSchemaResp,
            Map<String, ItemUseResp> typeIdAndStatPair) {
        List<DimSchemaResp> dimensions = dataSetSchemaResp.getDimensions();
        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        dimensions.forEach(dim -> {
            dim.setUseCnt(getItemCnt(dim, typeIdAndStatPair));
        });
        dataSetSchemaResp.setDimensions(dimensions);
    }

    private Long getItemCnt(SchemaItem schemaItem, Map<String, ItemUseResp> typeIdAndStatPair) {
        String bizNameKey = schemaItem.getTypeEnum().name().toLowerCase() + AT_SYMBOL + AT_SYMBOL
                + schemaItem.getBizName();
        String nameKey = schemaItem.getTypeEnum().name().toLowerCase() + AT_SYMBOL + AT_SYMBOL
                + schemaItem.getName();
        if (typeIdAndStatPair.containsKey(bizNameKey)) {
            return typeIdAndStatPair.get(bizNameKey).getUseCnt();
        }
        if (typeIdAndStatPair.containsKey(nameKey)) {
            return typeIdAndStatPair.get(nameKey).getUseCnt();
        }
        return 0L;
    }

    @Override
    public DimensionResp getDimension(String bizName, Long modelId) {
        return dimensionService.getDimension(bizName, modelId);
    }

    @Override
    public DimensionResp getDimension(Long id) {
        return dimensionService.getDimension(id);
    }

    @Override
    public List<DimensionResp> getDimensions(MetaFilter metaFilter) {
        return dimensionService.getDimensions(metaFilter);
    }

    @Override
    public List<MetricResp> getMetrics(MetaFilter metaFilter) {
        return metricService.getMetrics(metaFilter);
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
    public List<ModelResp> getModelList(List<Long> modelIds) {
        List<ModelResp> modelRespList = new ArrayList<>();
        if (!org.apache.commons.collections.CollectionUtils.isEmpty(modelIds)) {
            modelIds.stream().forEach(m -> {
                modelRespList.add(modelService.getModel(m));
            });
        }
        return modelRespList;
    }

    public SemanticSchemaResp buildSemanticSchema(SchemaFilterReq schemaFilterReq) {
        SemanticSchemaResp semanticSchemaResp = new SemanticSchemaResp();
        semanticSchemaResp.setDataSetId(schemaFilterReq.getDataSetId());
        semanticSchemaResp.setModelIds(schemaFilterReq.getModelIds());
        if (schemaFilterReq.getDataSetId() != null) {
            DataSetSchemaResp dataSetSchemaResp =
                    fetchDataSetSchema(schemaFilterReq.getDataSetId());
            BeanUtils.copyProperties(dataSetSchemaResp, semanticSchemaResp);
            semanticSchemaResp.setDataSetResp(dataSetSchemaResp);
            List<Long> modelIds = dataSetSchemaResp.getAllModels();
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setIds(modelIds);
            List<ModelResp> modelList = modelService.getModelList(metaFilter);
            metaFilter.setModelIds(modelIds);
            List<ModelRela> modelRelas = modelRelaService.getModelRela(modelIds);
            semanticSchemaResp.setModelResps(modelList);
            semanticSchemaResp.setModelRelas(modelRelas);
            semanticSchemaResp.setModelIds(modelIds);
            semanticSchemaResp.setSchemaType(SchemaType.DATASET);
        } else if (!CollectionUtils.isEmpty(schemaFilterReq.getModelIds())) {
            List<ModelSchemaResp> modelSchemaResps =
                    fetchModelSchemaResps(schemaFilterReq.getModelIds());
            semanticSchemaResp.setMetrics(modelSchemaResps.stream().map(ModelSchemaResp::getMetrics)
                    .flatMap(Collection::stream).collect(Collectors.toList()));
            semanticSchemaResp
                    .setDimensions(modelSchemaResps.stream().map(ModelSchemaResp::getDimensions)
                            .flatMap(Collection::stream).collect(Collectors.toList()));
            semanticSchemaResp
                    .setModelRelas(modelSchemaResps.stream().map(ModelSchemaResp::getModelRelas)
                            .flatMap(Collection::stream).collect(Collectors.toList()));
            semanticSchemaResp.setModelResps(
                    modelSchemaResps.stream().map(this::convert).collect(Collectors.toList()));
            semanticSchemaResp.setSchemaType(SchemaType.MODEL);
        }

        if (!CollectionUtils.isEmpty(semanticSchemaResp.getModelIds())) {
            DatabaseResp databaseResp =
                    modelService.getDatabaseByModelId(semanticSchemaResp.getModelIds().get(0));
            semanticSchemaResp.setDatabaseResp(databaseResp);
        }
        return semanticSchemaResp;
    }

    @Override
    public SemanticSchemaResp fetchSemanticSchema(SchemaFilterReq schemaFilterReq) {
        SemanticSchemaResp semanticSchemaResp = null;
        if (schemaCacheEnable) {
            semanticSchemaResp = semanticSchemaCache.getIfPresent(schemaFilterReq);
        }
        if (semanticSchemaResp == null) {
            semanticSchemaResp = buildSemanticSchema(schemaFilterReq);
            semanticSchemaCache.put(schemaFilterReq, semanticSchemaResp);
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
    public List<ItemResp> getDomainDataSetTree() {
        List<DomainResp> domainResps = domainService.getDomainList();
        List<ItemResp> itemResps = domainResps.stream().map(domain -> new ItemResp(domain.getId(),
                domain.getParentId(), domain.getName(), TypeEnums.DOMAIN))
                .collect(Collectors.toList());
        Map<Long, ItemResp> itemRespMap =
                itemResps.stream().collect(Collectors.toMap(ItemResp::getId, item -> item));
        List<DataSetResp> dataSetResps = dataSetService.getDataSetList(new MetaFilter());
        for (DataSetResp dataSetResp : dataSetResps) {
            ItemResp itemResp = itemRespMap.get(dataSetResp.getDomainId());
            if (itemResp != null) {
                ItemResp dataSet = new ItemResp(dataSetResp.getId(), dataSetResp.getDomainId(),
                        dataSetResp.getName(), TypeEnums.DATASET);
                itemResp.getChildren().add(dataSet);
            }
        }
        return itemResps.stream().filter(ItemResp::isRoot).collect(Collectors.toList());
    }

    private void fillStaticInfo(List<DataSetSchemaResp> dataSetSchemaResps) {
        List<Long> dataSetIds = dataSetSchemaResps.stream().map(DataSetSchemaResp::getId)
                .collect(Collectors.toList());
        ItemUseReq itemUseReq = new ItemUseReq();
        itemUseReq.setDataSetIds(dataSetIds);

        List<ItemUseResp> statInfos = getStatInfo(itemUseReq);
        log.debug("statInfos:{}", statInfos);
        fillCnt(dataSetSchemaResps, statInfos);
    }

    private Map<Long, DataSetResp> getDataSetMap(List<DataSetResp> dataSetResps) {
        if (CollectionUtils.isEmpty(dataSetResps)) {
            return new HashMap<>();
        }
        return dataSetResps.stream()
                .collect(Collectors.toMap(DataSetResp::getId, dataSetResp -> dataSetResp));
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

    @Override
    public void getSchemaYamlTpl(SemanticSchemaResp semanticSchemaResp,
            Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DataModelYamlTpl> dataModelYamlTplList, List<MetricYamlTpl> metricYamlTplList,
            Map<Long, String> modelIdName) {

        List<ModelResp> modelResps = semanticSchemaResp.getModelResps();
        if (org.apache.commons.collections.CollectionUtils.isEmpty(modelResps)) {
            return;
        }
        List<DimSchemaResp> dimensionResps = semanticSchemaResp.getDimensions();
        Long databaseId = modelResps.get(0).getDatabaseId();
        DatabaseResp databaseResp = databaseService.getDatabase(databaseId);
        for (ModelResp modelResp : modelResps) {
            modelIdName.put(modelResp.getId(), modelResp.getBizName());
            dataModelYamlTplList.add(ModelYamlManager.convert2YamlObj(modelResp, databaseResp));
            if (!dimensionYamlMap.containsKey(modelResp.getBizName())) {
                dimensionYamlMap.put(modelResp.getBizName(), new ArrayList<>());
            }
            List<DimensionResp> dimensionRespList = dimensionResps.stream()
                    .filter(d -> d.getModelBizName().equalsIgnoreCase(modelResp.getBizName()))
                    .collect(Collectors.toList());
            dimensionYamlMap.get(modelResp.getBizName())
                    .addAll(DimensionYamlManager.convert2DimensionYaml(dimensionRespList));
        }
        List<MetricResp> metricResps = new ArrayList<>(semanticSchemaResp.getMetrics());
        metricYamlTplList.addAll(MetricYamlManager.convert2YamlObj(metricResps));
    }

    @Override
    public ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric) {
        return modelService.getItemDate(dimension, metric);
    }

}
