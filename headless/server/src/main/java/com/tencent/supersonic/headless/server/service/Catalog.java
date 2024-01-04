package com.tencent.supersonic.headless.server.service;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Catalog {

    DatabaseResp getDatabase(Long id);

    DatabaseResp getDatabaseByModelId(Long modelId);

    DimensionResp getDimension(String bizName, Long modelId);

    DimensionResp getDimension(Long id);

    List<DimensionResp> getDimensions(MetaFilter metaFilter);

    List<MetricResp> getMetrics(MetaFilter metaFilter);

    MetricResp getMetric(Long id);

    List<ModelRela> getModelRela(List<Long> modelIds);

    void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DataModelYamlTpl> dataModelYamlTplList, List<MetricYamlTpl> metricYamlTplList,
            Map<Long, String> modelIdName);

    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

    List<ModelResp> getModelList(List<Long> modelIds);

}
