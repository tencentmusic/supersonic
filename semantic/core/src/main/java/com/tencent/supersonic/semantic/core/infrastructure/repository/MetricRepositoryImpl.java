package com.tencent.supersonic.semantic.core.infrastructure.repository;


import com.tencent.supersonic.semantic.core.domain.dataobject.MetricDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.MetricDOExample;
import com.tencent.supersonic.semantic.core.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.core.domain.repository.MetricRepository;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.MetricDOCustomMapper;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.MetricDOMapper;
import java.util.List;
import org.springframework.stereotype.Component;


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
        metricDOMapper.updateByPrimaryKeyWithBLOBs(metricDO);
    }

    @Override
    public List<MetricDO> getMetricList(Long domainId) {
        MetricDOExample metricDOExample = new MetricDOExample();
        metricDOExample.createCriteria().andDomainIdEqualTo(domainId);
        return metricDOMapper.selectByExampleWithBLOBs(metricDOExample);
    }

    @Override
    public List<MetricDO> getMetricListByIds(List<Long> ids) {
        MetricDOExample metricDOExample = new MetricDOExample();
        metricDOExample.createCriteria().andIdIn(ids);
        return metricDOMapper.selectByExampleWithBLOBs(metricDOExample);
    }

    @Override
    public MetricDO getMetricById(Long id) {
        return metricDOMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<MetricDO> getAllMetricList() {
        return metricDOMapper.selectByExampleWithBLOBs(new MetricDOExample());
    }

    @Override
    public List<MetricDO> getMetric(MetricFilter metricFilter) {
        MetricDOExample metricDOExample = new MetricDOExample();
        metricDOExample.createCriteria();
        if (metricFilter.getId() != null) {
            metricDOExample.getOredCriteria().get(0).andIdEqualTo(metricFilter.getId());
        }
        if (metricFilter.getName() != null) {
            metricDOExample.getOredCriteria().get(0).andNameLike("%" + metricFilter.getName() + "%");
        }
        if (metricFilter.getBizName() != null) {
            metricDOExample.getOredCriteria().get(0).andBizNameLike("%" + metricFilter.getBizName() + "%");
        }
        if (metricFilter.getCreatedBy() != null) {
            metricDOExample.getOredCriteria().get(0).andCreatedByEqualTo(metricFilter.getCreatedBy());
        }
        if (metricFilter.getDomainId() != null) {
            metricDOExample.getOredCriteria().get(0).andDomainIdEqualTo(metricFilter.getDomainId());
        }
        if (metricFilter.getSensitiveLevel() != null) {
            metricDOExample.getOredCriteria().get(0).andSensitiveLevelEqualTo(metricFilter.getSensitiveLevel());
        }
        if (metricFilter.getStatus() != null) {
            metricDOExample.getOredCriteria().get(0).andStatusEqualTo(metricFilter.getStatus());
        }
        return metricDOMapper.selectByExampleWithBLOBs(metricDOExample);
    }

    @Override
    public void deleteMetric(Long id) {
        metricDOMapper.deleteByPrimaryKey(id);
    }


}
