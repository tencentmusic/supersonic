package com.tencent.supersonic.headless.model.domain.repository;


import com.tencent.supersonic.headless.model.domain.dataobject.DomainDO;

import java.util.List;


public interface DomainRepository {

    void createDomain(DomainDO metaDomainDO);

    void updateDomain(DomainDO metaDomainDO);

    void deleteDomain(Long id);

    List<DomainDO> getDomainList();

    DomainDO getDomainById(Long id);

}
