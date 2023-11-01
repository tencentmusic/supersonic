package com.tencent.supersonic.semantic.model.domain;

import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Catalog {

    DatabaseResp getDatabase(Long id);

    DatabaseResp getDatabaseByModelId(Long modelId);

    List<DatasourceResp> getDatasourceList(Long modelId);

    String getModelFullPath(Long modelId);

    Map<Long, String> getModelFullPath();

    DimensionResp getDimension(String bizName, Long modelId);

    List<DimensionResp> getDimensions(Long modelId);

    List<MetricResp> getMetrics(Long modelId);

    void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DatasourceYamlTpl> datasourceYamlTplList, List<MetricYamlTpl> metricYamlTplList);


    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

    String getAgg(List<MetricResp> metricResps, List<MeasureResp> measureRespList, String metricBizName);

}
