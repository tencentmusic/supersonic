package com.tencent.supersonic.semantic.core.domain.repository;


import com.tencent.supersonic.semantic.core.domain.dataobject.DomainDO;

import java.util.List;


public interface DomainRepository {


    void createDomain(DomainDO metaDomainDO);

    void updateDomain(DomainDO metaDomainDO);

    void deleteDomain(Long id);

    List<DomainDO> getDomainList();

    DomainDO getDomainById(Long id);

}
