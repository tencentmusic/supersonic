package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.server.persistence.dataobject.DatabaseDO;

import java.util.List;

public interface DatabaseRepository {

    void createDatabase(DatabaseDO databaseDO);

    void updateDatabase(DatabaseDO databaseDO);

    DatabaseDO getDatabase(Long id);

    List<DatabaseDO> getDatabaseList();

    void deleteDatabase(Long id);
}
