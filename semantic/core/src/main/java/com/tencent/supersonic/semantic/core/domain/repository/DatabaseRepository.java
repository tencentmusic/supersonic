package com.tencent.supersonic.semantic.core.domain.repository;

import com.tencent.supersonic.semantic.core.domain.dataobject.DatabaseDO;
import java.util.List;

public interface DatabaseRepository {


    void createDatabase(DatabaseDO databaseDO);

    void updateDatabase(DatabaseDO databaseDO);

    DatabaseDO getDatabase(Long id);

    List<DatabaseDO> getDatabaseByDomainId(Long domainId);

}
