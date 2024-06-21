package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.ClassDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ClassMapper;
import com.tencent.supersonic.headless.server.persistence.repository.ClassRepository;
import com.tencent.supersonic.headless.server.pojo.ClassFilter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class ClassRepositoryImpl implements ClassRepository {

    private final ClassMapper mapper;

    public ClassRepositoryImpl(ClassMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Long create(ClassDO classDO) {
        mapper.insert(classDO);
        return classDO.getId();
    }

    @Override
    public Long update(ClassDO classDO) {
        mapper.updateById(classDO);
        return classDO.getId();
    }

    @Override
    public Integer delete(List<Long> ids) {
        return mapper.deleteBatchIds(ids);
    }

    @Override
    public ClassDO getClassById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<ClassDO> getClassDOList(ClassFilter filter) {
        QueryWrapper<ClassDO> wrapper = new QueryWrapper();
        if (Objects.nonNull(filter.getDomainId())) {
            wrapper.lambda().eq(ClassDO::getDomainId, filter.getDomainId());
        }
        if (Objects.nonNull(filter.getDataSetId())) {
            wrapper.lambda().eq(ClassDO::getDataSetId, filter.getDataSetId());
        }
        if (Strings.isNotEmpty(filter.getType())) {
            wrapper.lambda().eq(ClassDO::getType, filter.getType());
        }
        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            wrapper.lambda().in(ClassDO::getId, filter.getIds());
        }
        if (Objects.nonNull(filter.getCreatedBy())) {
            wrapper.lambda().eq(ClassDO::getCreatedBy, filter.getCreatedBy());
        }
        if (Objects.nonNull(filter.getStatus())) {
            wrapper.lambda().eq(ClassDO::getStatus, filter.getStatus());
        }
        if (Objects.nonNull(filter.getBizName())) {
            wrapper.lambda().eq(ClassDO::getBizName, filter.getBizName());
        }
        return mapper.selectList(wrapper);
    }

    @Override
    public List<ClassDO> getAllClassDOList() {
        QueryWrapper<ClassDO> wrapper = new QueryWrapper();
        return mapper.selectList(wrapper);
    }
}