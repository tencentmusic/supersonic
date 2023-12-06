package com.tencent.supersonic.semantic.model.infrastructure.repository;

import com.tencent.supersonic.semantic.model.domain.dataobject.MetricDO;
import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.model.domain.repository.MetricRepository;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.MetricDOCustomMapper;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.MetricDOMapper;
import org.springframework.stereotype.Component;
import java.util.List;


@Component
public class MetricRepositoryImpl implements MetricRepository {


    private MetricDOMapper metricDOMapper;

    private MetricDOCustomMapper metricDOCustomMapper;

    public MetricRepositoryImpl(MetricDOMapper metricDOMapper,
                                MetricDOCustomMapper metricDOCustomMapper) {
        this.metricDOMapper = metricDOMapper;
        this.metricDOCustomMapper = metricDOCustomMapper;
    }

    @Override
    public Long createMetric(MetricDO metricDO) {
        metricDOMapper.insert(metricDO);
        return metricDO.getId();
    }

    @Override
    public void createMetricBatch(List<MetricDO> metricDOS) {
        metricDOCustomMapper.batchInsert(metricDOS);
    }

    @Override
    public void updateMetric(MetricDO metricDO) {
        metricDOMapper.updateById(metricDO);
    }

    @Override
    public void batchUpdateStatus(List<MetricDO> metricDOS) {
        metricDOCustomMapper.batchUpdateStatus(metricDOS);
    }

    @Override
    public MetricDO getMetricById(Long id) {
        return metricDOMapper.selectById(id);
    }

    @Override
    public List<MetricDO> getMetric(MetricFilter metricFilter) {
        return metricDOCustomMapper.query(metricFilter);
    }

}
