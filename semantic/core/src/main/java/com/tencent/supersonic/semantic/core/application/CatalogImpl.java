package com.tencent.supersonic.semantic.core.application;

import com.tencent.supersonic.semantic.api.core.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.api.core.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.core.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.ItemDateResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.core.domain.Catalog;
import com.tencent.supersonic.semantic.core.domain.DatasourceService;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.core.domain.MetricService;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatabaseDO;
import com.tencent.supersonic.semantic.core.domain.repository.DatabaseRepository;
import com.tencent.supersonic.semantic.core.domain.utils.DatabaseConverter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CatalogImpl implements Catalog {

    private final DatabaseRepository databaseRepository;
    private final DomainService domainService;
    private final DimensionService dimensionService;
    private final DatasourceService datasourceService;
    private final MetricService metricService;

    public CatalogImpl(DatabaseRepository databaseRepository,
            DomainService domainService, DimensionService dimensionService,
            DatasourceService datasourceService,
            MetricService metricService) {
        this.databaseRepository = databaseRepository;
        this.domainService = domainService;
        this.dimensionService = dimensionService;
        this.datasourceService = datasourceService;
        this.metricService = metricService;
    }

    public DatabaseResp getDatabase(Long id) {
        DatabaseDO databaseDO = databaseRepository.getDatabase(id);
        return DatabaseConverter.convert(databaseDO);
    }

    @Override
    public String getDomainFullPath(Long domainId) {
        return domainService.getDomainFullPath(domainId);
    }

    @Override
    public Map<Long, String> getDomainFullPath() {
        return domainService.getDomainFullPath();
    }

    @Override
    public DimensionResp getDimension(String bizName, Long domainId) {
        return dimensionService.getDimension(bizName, domainId);
    }

    @Override
    public void getModelYamlTplByDomainIds(Set<Long> domainIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DatasourceYamlTpl> datasourceYamlTplList, List<MetricYamlTpl> metricYamlTplList) {
        datasourceService.getModelYamlTplByDomainIds(domainIds, dimensionYamlMap, datasourceYamlTplList,
                metricYamlTplList);
    }

    @Override
    public List<DimensionResp> getDimensions(Long domainId) {
        return dimensionService.getDimensions(domainId);
    }

    @Override
    public List<DatasourceResp> getDatasourceList(Long domainId) {
        return datasourceService.getDatasourceList(domainId);
    }

    @Override
    public List<MetricResp> getMetrics(Long domainId) {
        return metricService.getMetrics(domainId);
    }

    @Override
    public ItemDateResp getDateDate(ItemDateFilter dimension, ItemDateFilter metric) {
        return datasourceService.getDateDate(dimension, metric);
    }
}
