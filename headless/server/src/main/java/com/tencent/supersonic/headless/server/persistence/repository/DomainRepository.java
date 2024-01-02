package com.tencent.supersonic.headless.server.persistence.repository;


import com.tencent.supersonic.headless.server.persistence.dataobject.DomainDO;

import java.util.List;

public interface DomainRepository {

    void createDomain(DomainDO metaDomainDO);

    void updateDomain(DomainDO metaDomainDO);

    void deleteDomain(Long id);

    List<DomainDO> getDomainList();

    DomainDO getDomainById(Long id);

}
