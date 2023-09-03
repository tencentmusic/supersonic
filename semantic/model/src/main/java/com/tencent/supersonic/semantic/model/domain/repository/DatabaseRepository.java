package com.tencent.supersonic.semantic.model.domain.repository;

import com.tencent.supersonic.semantic.model.domain.dataobject.DatabaseDO;
import java.util.List;

public interface DatabaseRepository {


    void createDatabase(DatabaseDO databaseDO);

    void updateDatabase(DatabaseDO databaseDO);

    DatabaseDO getDatabase(Long id);

    List<DatabaseDO> getDatabaseList();

    void deleteDatabase(Long id);
}
