package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.server.manager.DimensionYamlManager;
import com.tencent.supersonic.headless.server.manager.MetricYamlManager;
import com.tencent.supersonic.headless.server.manager.ModelYamlManager;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.headless.server.service.Catalog;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CatalogImpl implements Catalog {

    private final DatabaseService databaseService;
    private final ModelService modelService;
    private final DimensionService dimensionService;
    private final MetricService metricService;
    private final ModelRelaService modelRelaService;
    private final DataSetService dataSetService;
    private final SchemaService schemaService;

    public CatalogImpl(DatabaseService databaseService, SchemaService schemaService,
            ModelService modelService, DimensionService dimensionService, DataSetService dataSetService,
            MetricService metricService, ModelRelaService modelRelaService) {
        this.databaseService = databaseService;
        this.modelService = modelService;
        this.dimensionService = dimensionService;
        this.dataSetService = dataSetService;
        this.metricService = metricService;
        this.modelRelaService = modelRelaService;
        this.schemaService = schemaService;
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
    public MetricResp getMetric(Long id) {
        return metricService.getMetric(id);
    }

    @Override
    public ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric) {
        return modelService.getItemDate(dimension, metric);
    }

    @Override
    public List<ModelResp> getModelList(List<Long> modelIds) {
        List<ModelResp> modelRespList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(modelIds)) {
            modelIds.stream().forEach(m -> {
                modelRespList.add(modelService.getModel(m));
            });
        }
        return modelRespList;
    }

    @Override
    public void getSchemaYamlTpl(SemanticSchemaResp semanticSchemaResp,
                                   Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
                                   List<DataModelYamlTpl> dataModelYamlTplList,
                                   List<MetricYamlTpl> metricYamlTplList,
                                   Map<Long, String> modelIdName) {

        List<ModelResp> modelResps = semanticSchemaResp.getModelResps();
        if (CollectionUtils.isEmpty(modelResps)) {
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
            dimensionYamlMap.get(modelResp.getBizName()).addAll(DimensionYamlManager.convert2DimensionYaml(
                    dimensionRespList));
        }
        List<MetricResp> metricResps = new ArrayList<>(semanticSchemaResp.getMetrics());
        metricYamlTplList.addAll(MetricYamlManager.convert2YamlObj(metricResps));
    }

    @Override
    public SemanticSchemaResp fetchSemanticSchema(SchemaFilterReq schemaFilterReq) {
        return schemaService.fetchSemanticSchema(schemaFilterReq);
    }

    @Override
    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) throws ExecutionException {
        return schemaService.getStatInfo(itemUseReq);
    }

}
