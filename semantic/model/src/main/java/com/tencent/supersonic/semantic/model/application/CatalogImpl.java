package com.tencent.supersonic.semantic.model.application;

import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.ItemDateResp;
import com.tencent.supersonic.semantic.api.model.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CatalogImpl implements Catalog {

    private final DatabaseService databaseService;
    private final ModelService modelService;
    private final DimensionService dimensionService;
    private final DatasourceService datasourceService;
    private final MetricService metricService;

    public CatalogImpl(DatabaseService databaseService,
            ModelService modelService, DimensionService dimensionService,
            DatasourceService datasourceService,
            MetricService metricService) {
        this.databaseService = databaseService;
        this.modelService = modelService;
        this.dimensionService = dimensionService;
        this.datasourceService = datasourceService;
        this.metricService = metricService;
    }

    public DatabaseResp getDatabase(Long id) {
        return databaseService.getDatabase(id);
    }

    public DatabaseResp getDatabaseByModelId(Long modelId) {
        return modelService.getDatabaseByModelId(modelId);
    }

    @Override
    public String getModelFullPath(Long modelId) {
        ModelResp modelResp = modelService.getModelMap().get(modelId);
        if (modelResp != null) {
            return modelResp.getFullPath();
        }
        return "";
    }

    @Override
    public Map<Long, String> getModelFullPath() {
        return modelService.getModelFullPathMap();
    }

    @Override
    public DimensionResp getDimension(String bizName, Long modelId) {
        return dimensionService.getDimension(bizName, modelId);
    }

    @Override
    public void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DatasourceYamlTpl> datasourceYamlTplList, List<MetricYamlTpl> metricYamlTplList) {
        datasourceService.getModelYamlTplByModelIds(modelIds, dimensionYamlMap, datasourceYamlTplList,
                metricYamlTplList);
    }

    @Override
    public List<DimensionResp> getDimensions(Long modelId) {
        return dimensionService.getDimensions(modelId);
    }

    @Override
    public List<DatasourceResp> getDatasourceList(Long modelId) {
        return datasourceService.getDatasourceList(modelId);
    }

    @Override
    public List<MetricResp> getMetrics(Long modelId) {
        return metricService.getMetrics(modelId);
    }

    @Override
    public ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric) {
        return datasourceService.getItemDate(dimension, metric);
    }
}
