package com.tencent.supersonic.semantic.core.domain;

import com.tencent.supersonic.semantic.api.core.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.api.core.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.core.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.ItemDateResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Catalog {

    DatabaseResp getDatabase(Long id);

    List<DatasourceResp> getDatasourceList(Long domainId);

    String getDomainFullPath(Long domainId);

    Map<Long, String> getDomainFullPath();

    DimensionResp getDimension(String bizName, Long domainId);

    List<DimensionResp> getDimensions(Long domainId);

    List<MetricResp> getMetrics(Long domainId);

    void getModelYamlTplByDomainIds(Set<Long> domainIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DatasourceYamlTpl> datasourceYamlTplList, List<MetricYamlTpl> metricYamlTplList);


    ItemDateResp getDateDate(ItemDateFilter dimension, ItemDateFilter metric);

}
