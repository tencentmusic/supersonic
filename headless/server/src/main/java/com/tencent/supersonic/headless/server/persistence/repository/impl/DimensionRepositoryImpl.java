package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.DimensionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DimensionDOCustomMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.DimensionDOMapper;
import com.tencent.supersonic.headless.server.persistence.repository.DimensionRepository;
import com.tencent.supersonic.headless.server.pojo.DimensionFilter;
import com.tencent.supersonic.headless.server.pojo.DimensionsFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DimensionRepositoryImpl implements DimensionRepository {

    private DimensionDOMapper dimensionDOMapper;

    private DimensionDOCustomMapper dimensionDOCustomMapper;

    public DimensionRepositoryImpl(DimensionDOMapper dimensionDOMapper,
            DimensionDOCustomMapper dimensionDOCustomMapper) {
        this.dimensionDOMapper = dimensionDOMapper;
        this.dimensionDOCustomMapper = dimensionDOCustomMapper;
    }

    @Override
    public void createDimension(DimensionDO dimensionDO) {
        dimensionDOMapper.insert(dimensionDO);
    }

    @Override
    public void createDimensionBatch(List<DimensionDO> dimensionDOS) {
        dimensionDOCustomMapper.batchInsert(dimensionDOS);
    }

    @Override
    public void updateDimension(DimensionDO dimensionDO) {
        dimensionDOMapper.updateById(dimensionDO);
    }

    @Override
    public void batchUpdateStatus(List<DimensionDO> dimensionDOS) {
        dimensionDOCustomMapper.batchUpdateStatus(dimensionDOS);
    }

    @Override
    public DimensionDO getDimensionById(Long id) {
        return dimensionDOMapper.selectById(id);
    }

    @Override
    public List<DimensionDO> getDimension(DimensionFilter dimensionFilter) {
        QueryWrapper<DimensionDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", 3);
        if (Objects.nonNull(dimensionFilter.getIds()) && !dimensionFilter.getIds().isEmpty()) {
            queryWrapper.in("id", dimensionFilter.getIds());
        }
        if (StringUtils.isNotBlank(dimensionFilter.getId())) {
            queryWrapper.eq("id", dimensionFilter.getId());
        }
        if (Objects.nonNull(dimensionFilter.getModelIds())
                && !dimensionFilter.getModelIds().isEmpty()) {
            queryWrapper.in("model_id", dimensionFilter.getModelIds());
        }
        if (StringUtils.isNotBlank(dimensionFilter.getName())) {
            queryWrapper.like("name", dimensionFilter.getName());
        }
        if (StringUtils.isNotBlank(dimensionFilter.getId())) {
            queryWrapper.like("biz_name", dimensionFilter.getBizName());
        }
        if (Objects.nonNull(dimensionFilter.getStatus())) {
            queryWrapper.eq("status", dimensionFilter.getStatus());
        }
        if (Objects.nonNull(dimensionFilter.getSensitiveLevel())) {
            queryWrapper.eq("sensitive_level", dimensionFilter.getSensitiveLevel());
        }
        if (StringUtils.isNotBlank(dimensionFilter.getCreatedBy())) {
            queryWrapper.eq("created_by", dimensionFilter.getCreatedBy());
        }
        if (StringUtils.isNotBlank(dimensionFilter.getKey())) {
            String key = dimensionFilter.getKey();
            queryWrapper.like("name", key).or().like("biz_name", key).or().like("description", key)
                    .or().like("alias", key).or().like("created_by", key);
        }

        return dimensionDOMapper.selectList(queryWrapper);
    }

    @Override
    public List<DimensionDO> getDimensions(DimensionsFilter dimensionsFilter) {
        return dimensionDOCustomMapper.queryDimensions(dimensionsFilter);
    }
}
