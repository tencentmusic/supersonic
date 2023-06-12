package com.tencent.supersonic.semantic.core.infrastructure.repository;

import com.tencent.supersonic.semantic.core.domain.dataobject.DatabaseDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatabaseDOExample;
import com.tencent.supersonic.semantic.core.domain.repository.DatabaseRepository;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.DatabaseDOMapper;
import java.util.List;
import org.springframework.stereotype.Component;


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
    public List<DatabaseDO> getDatabaseByDomainId(Long domainId) {
        DatabaseDOExample databaseDOExample = new DatabaseDOExample();
        databaseDOExample.createCriteria().andDomainIdEqualTo(domainId);
        return databaseDOMapper.selectByExampleWithBLOBs(databaseDOExample);

    }


}
