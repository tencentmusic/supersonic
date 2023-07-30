package com.tencent.supersonic.semantic.model.infrastructure.repository;

import com.tencent.supersonic.semantic.model.domain.dataobject.DomainDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DomainDOExample;
import com.tencent.supersonic.semantic.model.domain.repository.DomainRepository;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.DomainDOMapper;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class DomainRepositoryImpl implements DomainRepository {

    private DomainDOMapper domainDOMapper;

    public DomainRepositoryImpl(DomainDOMapper domainDOMapper) {
        this.domainDOMapper = domainDOMapper;
    }

    @Override
    public void createDomain(DomainDO metaDomainDO) {
        domainDOMapper.insert(metaDomainDO);
    }

    @Override
    public void updateDomain(DomainDO metaDomainDO) {
        domainDOMapper.updateByPrimaryKey(metaDomainDO);
    }

    @Override
    public void deleteDomain(Long id) {
        domainDOMapper.deleteByPrimaryKey(id);
    }

    @Override
    public List<DomainDO> getDomainList() {
        DomainDOExample metaDomainDOExample = new DomainDOExample();
        return domainDOMapper.selectByExample(metaDomainDOExample);
    }

    @Override
    public DomainDO getDomainById(Long id) {
        return domainDOMapper.selectByPrimaryKey(id);
    }


}
