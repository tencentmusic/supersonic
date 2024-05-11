package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricQueryDefaultConfigDO;
import com.tencent.supersonic.headless.server.persistence.mapper.MetricDOCustomMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.MetricDOMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.MetricQueryDefaultConfigDOMapper;
import com.tencent.supersonic.headless.server.persistence.repository.MetricRepository;
import com.tencent.supersonic.headless.server.pojo.MetricFilter;
import com.tencent.supersonic.headless.server.pojo.MetricsFilter;
import org.springframework.stereotype.Component;
import java.util.List;


@Component
public class MetricRepositoryImpl implements MetricRepository {


    private MetricDOMapper metricDOMapper;

    private MetricDOCustomMapper metricDOCustomMapper;

    private MetricQueryDefaultConfigDOMapper metricQueryDefaultConfigDOMapper;

    public MetricRepositoryImpl(MetricDOMapper metricDOMapper,
                                MetricDOCustomMapper metricDOCustomMapper,
                                MetricQueryDefaultConfigDOMapper metricQueryDefaultConfigDOMapper) {
        this.metricDOMapper = metricDOMapper;
        this.metricDOCustomMapper = metricDOCustomMapper;
        this.metricQueryDefaultConfigDOMapper = metricQueryDefaultConfigDOMapper;
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
    public void batchPublish(List<MetricDO> metricDOS) {
        metricDOCustomMapper.batchPublish(metricDOS);
    }

    @Override
    public void batchUnPublish(List<MetricDO> metricDOS) {
        metricDOCustomMapper.batchUnPublish(metricDOS);
    }

    @Override
    public void updateClassificationsBatch(List<MetricDO> metricDOS) {
        metricDOCustomMapper.updateClassificationsBatch(metricDOS);
    }

    @Override
    public MetricDO getMetricById(Long id) {
        return metricDOMapper.selectById(id);
    }

    @Override
    public List<MetricDO> getMetric(MetricFilter metricFilter) {
        return metricDOCustomMapper.query(metricFilter);
    }

    @Override
    public List<MetricDO> getMetrics(MetricsFilter metricsFilter) {
        return metricDOCustomMapper.queryMetrics(metricsFilter);
    }

    @Override
    public void saveDefaultQueryConfig(MetricQueryDefaultConfigDO defaultConfigDO) {
        metricQueryDefaultConfigDOMapper.insert(defaultConfigDO);
    }

    @Override
    public void updateDefaultQueryConfig(MetricQueryDefaultConfigDO defaultConfigDO) {
        metricQueryDefaultConfigDOMapper.updateById(defaultConfigDO);
    }

    @Override
    public MetricQueryDefaultConfigDO getDefaultQueryConfig(Long metricId, String userName) {
        QueryWrapper<MetricQueryDefaultConfigDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MetricQueryDefaultConfigDO::getMetricId, metricId)
                .eq(MetricQueryDefaultConfigDO::getCreatedBy, userName);
        return metricQueryDefaultConfigDOMapper.selectOne(queryWrapper);
    }

}
