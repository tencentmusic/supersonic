package com.tencent.supersonic.headless.model.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.model.domain.dataobject.MetricDO;
import com.tencent.supersonic.headless.model.domain.pojo.MetricFilter;
import com.tencent.supersonic.headless.model.infrastructure.mapper.MetricDOCustomMapper;
import com.tencent.supersonic.headless.model.infrastructure.mapper.MetricDOMapper;
import com.tencent.supersonic.headless.model.domain.dataobject.MetricQueryDefaultConfigDO;
import com.tencent.supersonic.headless.model.domain.repository.MetricRepository;
import com.tencent.supersonic.headless.model.infrastructure.mapper.MetricQueryDefaultConfigDOMapper;
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
    public MetricDO getMetricById(Long id) {
        return metricDOMapper.selectById(id);
    }

    @Override
    public List<MetricDO> getMetric(MetricFilter metricFilter) {
        return metricDOCustomMapper.query(metricFilter);
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
