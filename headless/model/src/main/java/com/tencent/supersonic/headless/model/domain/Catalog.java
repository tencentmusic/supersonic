package com.tencent.supersonic.headless.model.domain;

import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.headless.common.model.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.common.model.response.DatabaseResp;
import com.tencent.supersonic.headless.common.model.response.DimensionResp;
import com.tencent.supersonic.headless.common.model.response.MetricResp;
import com.tencent.supersonic.headless.common.model.response.ModelResp;
import com.tencent.supersonic.headless.common.model.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.common.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.common.model.yaml.MetricYamlTpl;

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

    List<ModelResp> getModelList(List<Long> modelIds);

}
