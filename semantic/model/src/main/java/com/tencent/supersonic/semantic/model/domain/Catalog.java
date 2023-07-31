package com.tencent.supersonic.semantic.model.domain;

import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.ItemDateResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Catalog {

    DatabaseResp getDatabase(Long id);
    DatabaseResp getDatabaseByDomainId(Long domainId);

    List<DatasourceResp> getDatasourceList(Long domainId);

    String getDomainFullPath(Long domainId);

    Map<Long, String> getDomainFullPath();

    DimensionResp getDimension(String bizName, Long domainId);

    List<DimensionResp> getDimensions(Long domainId);

    List<MetricResp> getMetrics(Long domainId);

    void getModelYamlTplByDomainIds(Set<Long> domainIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DatasourceYamlTpl> datasourceYamlTplList, List<MetricYamlTpl> metricYamlTplList);


    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

}
