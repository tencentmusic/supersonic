package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.tencent.supersonic.headless.server.persistence.dataobject.DatabaseDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DatabaseDOExample;
import com.tencent.supersonic.headless.server.persistence.mapper.DatabaseDOMapper;
import com.tencent.supersonic.headless.server.persistence.repository.DatabaseRepository;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class DatabaseRepositoryImpl implements DatabaseRepository {


    private DatabaseDOMapper databaseDOMapper;


    public DatabaseRepositoryImpl(DatabaseDOMapper databaseDOMapper) {
        this.databaseDOMapper = databaseDOMapper;
    }

    @Override
    public void createDatabase(DatabaseDO databaseDO) {
        databaseDOMapper.insert(databaseDO);
    }

    @Override
    public void updateDatabase(DatabaseDO databaseDO) {
        databaseDOMapper.updateByPrimaryKeyWithBLOBs(databaseDO);
    }

    @Override
    public DatabaseDO getDatabase(Long id) {
        return databaseDOMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<DatabaseDO> getDatabaseList() {
        return databaseDOMapper.selectByExampleWithBLOBs(new DatabaseDOExample());
    }

    @Override
    public void deleteDatabase(Long id) {
        databaseDOMapper.deleteByPrimaryKey(id);
    }

}
