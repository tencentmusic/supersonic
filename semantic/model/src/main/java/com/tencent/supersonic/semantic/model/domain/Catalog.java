package com.tencent.supersonic.semantic.model.domain;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.yaml.DataModelYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Catalog {

    DatabaseResp getDatabase(Long id);

    DatabaseResp getDatabaseByModelId(Long modelId);

    String getModelFullPath(Long modelId);

    String getModelFullPath(List<Long> modelIds);

    DimensionResp getDimension(String bizName, Long modelId);

    List<DimensionResp> getDimensions(List<Long> modelIds);

    List<MetricResp> getMetrics(List<Long> modelIds);

    List<ModelRela> getModelRela(List<Long> modelIds);

    void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DataModelYamlTpl> dataModelYamlTplList, List<MetricYamlTpl> metricYamlTplList,
            Map<Long, String> modelIdName);

    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

}
