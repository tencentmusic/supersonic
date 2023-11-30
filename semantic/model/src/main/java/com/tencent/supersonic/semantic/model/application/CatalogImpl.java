package com.tencent.supersonic.semantic.model.application;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.yaml.DataModelYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelRelaService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public String getModelFullPath(Long modelId) {
        ModelResp modelResp = modelService.getModel(modelId);
        if (modelResp != null) {
            return modelResp.getFullPath();
        }
        return "";
    }

    @Override
    public String getModelFullPath(List<Long> modelIds) {
        return String.join(",", modelIds.stream().map(Object::toString).collect(Collectors.toList()));
    }

    @Override
    public DimensionResp getDimension(String bizName, Long modelId) {
        return dimensionService.getDimension(bizName, modelId);
    }

    @Override
    public List<ModelRela> getModelRela(List<Long> modelIds) {
        return modelRelaService.getModelRela(modelIds);
    }

    @Override
    public List<DimensionResp> getDimensions(List<Long> modelIds) {
        MetaFilter metricFilter = new MetaFilter(modelIds);
        metricFilter.setStatus(StatusEnum.ONLINE.getCode());
        return dimensionService.getDimensions(metricFilter);
    }

    @Override
    public List<MetricResp> getMetrics(List<Long> modelIds) {
        MetaFilter metricFilter = new MetaFilter(modelIds);
        return metricService.getMetrics(metricFilter);
    }

    @Override
    public ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric) {
        return datasourceService.getItemDate(dimension, metric);
    }

    @Override
    public void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DataModelYamlTpl> dataModelYamlTplList, List<MetricYamlTpl> metricYamlTplList,
            Map<Long, String> modelIdName) {
        datasourceService.getModelYamlTplByModelIds(modelIds, dimensionYamlMap, dataModelYamlTplList,
                metricYamlTplList, modelIdName);
    }

}
