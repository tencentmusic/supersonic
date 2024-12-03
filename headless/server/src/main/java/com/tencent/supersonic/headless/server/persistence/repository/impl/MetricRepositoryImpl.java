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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

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
        QueryWrapper<MetricDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().ne(MetricDO::getStatus, 3);
        if (Objects.nonNull(metricFilter.getIds()) && !metricFilter.getIds().isEmpty()) {
            queryWrapper.lambda().in(MetricDO::getId, metricFilter.getIds());
        }
        if (StringUtils.isNotBlank(metricFilter.getId())) {
            queryWrapper.lambda().eq(MetricDO::getId, metricFilter.getId());
        }
        if (Objects.nonNull(metricFilter.getModelIds()) && !metricFilter.getModelIds().isEmpty()) {
            queryWrapper.lambda().in(MetricDO::getModelId, metricFilter.getModelIds());
        }
        if (StringUtils.isNotBlank(metricFilter.getType())) {
            queryWrapper.lambda().eq(MetricDO::getType, metricFilter.getType());
        }
        if (StringUtils.isNotBlank(metricFilter.getName())) {
            queryWrapper.lambda().like(MetricDO::getName, metricFilter.getName());
        }
        if (StringUtils.isNotBlank(metricFilter.getId())) {
            queryWrapper.lambda().like(MetricDO::getBizName, metricFilter.getBizName());
        }
        if (Objects.nonNull(metricFilter.getStatus())) {
            queryWrapper.lambda().eq(MetricDO::getStatus, metricFilter.getStatus());
        }
        if (Objects.nonNull(metricFilter.getSensitiveLevel())) {
            queryWrapper.lambda().eq(MetricDO::getSensitiveLevel, metricFilter.getSensitiveLevel());
        }
        if (StringUtils.isNotBlank(metricFilter.getCreatedBy())) {
            queryWrapper.lambda().eq(MetricDO::getCreatedBy, metricFilter.getCreatedBy());
        }
        if (Objects.nonNull(metricFilter.getIsPublish()) && metricFilter.getIsPublish() == 1) {
            queryWrapper.lambda().eq(MetricDO::getIsPublish, metricFilter.getIsPublish());
        }
        if (StringUtils.isNotBlank(metricFilter.getKey())) {
            String key = metricFilter.getKey();
            queryWrapper.lambda().like(MetricDO::getName, key).or().like(MetricDO::getBizName, key)
                    .or().like(MetricDO::getDescription, key).or().like(MetricDO::getAlias, key)
                    .or().like(MetricDO::getCreatedBy, key);
        }

        return metricDOMapper.selectList(queryWrapper);
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
