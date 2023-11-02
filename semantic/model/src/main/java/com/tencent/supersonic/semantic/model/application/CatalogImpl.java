package com.tencent.supersonic.semantic.model.application;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
        MetaFilter metricFilter = new MetaFilter(Lists.newArrayList(modelId));
        metricFilter.setStatus(StatusEnum.ONLINE.getCode());
        return dimensionService.getDimensions(metricFilter);
    }

    @Override
    public List<DatasourceResp> getDatasourceList(Long modelId) {
        return datasourceService.getDatasourceList(modelId);
    }

    @Override
    public List<MetricResp> getMetrics(Long modelId) {
        MetaFilter metricFilter = new MetaFilter(Lists.newArrayList(modelId));
        return metricService.getMetrics(metricFilter);
    }

    @Override
    public ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric) {
        return datasourceService.getItemDate(dimension, metric);
    }

    @Override
    public String getAgg(List<MetricResp> metricResps, List<MeasureResp> measureRespList, String metricBizName) {
        try {
            if (!CollectionUtils.isEmpty(metricResps)) {
                Optional<MetricResp> metric = metricResps.stream()
                        .filter(m -> m.getBizName().equalsIgnoreCase(metricBizName)).findFirst();
                if (metric.isPresent() && Objects.nonNull(metric.get().getTypeParams()) && !CollectionUtils.isEmpty(
                        metric.get().getTypeParams().getMeasures())) {
                    if (!CollectionUtils.isEmpty(measureRespList)) {
                        String measureName = metric.get().getTypeParams().getMeasures().get(0).getBizName();
                        Optional<MeasureResp> measure = measureRespList.stream()
                                .filter(Objects::nonNull)
                                .filter(m -> {
                                    if (StringUtils.isNotEmpty(m.getBizName())) {
                                        return m.getBizName().equalsIgnoreCase(measureName);
                                    }
                                    return false;
                                })
                                .findFirst();
                        if (measure.isPresent()) {
                            return measure.get().getAgg();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("getAgg:", e);
        }
        return "";
    }
}
