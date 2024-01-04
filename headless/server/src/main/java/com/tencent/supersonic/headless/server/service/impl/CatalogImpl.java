package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.api.response.DatabaseResp;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.core.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.core.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.core.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.Catalog;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class CatalogImpl implements Catalog {

    private final DatabaseService databaseService;
    private final ModelService modelService;
    private final DimensionService dimensionService;
    private final ModelService datasourceService;
    private final MetricService metricService;
    private final ModelRelaService modelRelaService;

    public CatalogImpl(DatabaseService databaseService,
            ModelService modelService, DimensionService dimensionService,
            ModelService datasourceService,
            MetricService metricService, ModelRelaService modelRelaService) {
        this.databaseService = databaseService;
        this.modelService = modelService;
        this.dimensionService = dimensionService;
        this.datasourceService = datasourceService;
        this.metricService = metricService;
        this.modelRelaService = modelRelaService;
    }

    public DatabaseResp getDatabase(Long id) {
        return databaseService.getDatabase(id);
    }

    public DatabaseResp getDatabaseByModelId(Long modelId) {
        return modelService.getDatabaseByModelId(modelId);
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
    public List<ModelRela> getModelRela(List<Long> modelIds) {
        return modelRelaService.getModelRela(modelIds);
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
        return datasourceService.getItemDate(dimension, metric);
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
    public void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DataModelYamlTpl> dataModelYamlTplList, List<MetricYamlTpl> metricYamlTplList,
            Map<Long, String> modelIdName) {
        datasourceService.getModelYamlTplByModelIds(modelIds, dimensionYamlMap, dataModelYamlTplList,
                metricYamlTplList, modelIdName);
    }

}
