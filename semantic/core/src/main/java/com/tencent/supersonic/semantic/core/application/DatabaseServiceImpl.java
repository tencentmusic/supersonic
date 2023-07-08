package com.tencent.supersonic.semantic.core.application;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.core.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.core.domain.adaptor.engineadapter.EngineAdaptor;
import com.tencent.supersonic.semantic.core.domain.adaptor.engineadapter.EngineAdaptorFactory;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatabaseDO;
import com.tencent.supersonic.semantic.core.domain.repository.DatabaseRepository;
import com.tencent.supersonic.semantic.core.domain.utils.DatabaseConverter;
import com.tencent.supersonic.semantic.core.domain.utils.JdbcDataSourceUtils;
import com.tencent.supersonic.semantic.core.domain.utils.SqlUtils;
import com.tencent.supersonic.semantic.core.domain.DatabaseService;
import com.tencent.supersonic.semantic.core.domain.pojo.Database;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class DatabaseServiceImpl implements DatabaseService {

    private final SqlUtils sqlUtils;
    private DatabaseRepository databaseRepository;

    public DatabaseServiceImpl(DatabaseRepository databaseRepository, SqlUtils sqlUtils) {
        this.databaseRepository = databaseRepository;
        this.sqlUtils = sqlUtils;
    }

    @Override
    public boolean testConnect(DatabaseReq databaseReq, User user) {
        Database database = DatabaseConverter.convert(databaseReq, user);
        return JdbcDataSourceUtils.testDatabase(database);
    }

    @Override
    public DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user) {
        Database database = DatabaseConverter.convert(databaseReq, user);
        Optional<DatabaseDO> databaseDOOptional = getDatabaseDO(databaseReq.getDomainId());
        if (databaseDOOptional.isPresent()) {
            DatabaseDO databaseDO = DatabaseConverter.convert(database, databaseDOOptional.get());
            databaseRepository.updateDatabase(databaseDO);
            return DatabaseConverter.convert(databaseDO);
        }
        DatabaseDO databaseDO = DatabaseConverter.convert(database);
        databaseRepository.createDatabase(databaseDO);
        return DatabaseConverter.convert(databaseDO);
    }


    @Override
    public DatabaseResp getDatabase(Long id) {
        DatabaseDO databaseDO = databaseRepository.getDatabase(id);
        return DatabaseConverter.convert(databaseDO);
    }

    @Override
    // one domain only has one database
    public DatabaseResp getDatabaseByDomainId(Long domainId) {
        Optional<DatabaseDO> databaseDO = getDatabaseDO(domainId);
        return databaseDO.map(DatabaseConverter::convert).orElse(null);
    }

    @Override
    public QueryResultWithSchemaResp executeSql(String sql, Long domainId) {
        DatabaseResp databaseResp = getDatabaseByDomainId(domainId);
        return executeSql(sql, databaseResp);
    }

    @Override
    public QueryResultWithSchemaResp executeSql(String sql, DatabaseResp databaseResp) {
        return queryWithColumns(sql, databaseResp);
    }

    @Override
    public QueryResultWithSchemaResp queryWithColumns(SqlParserResp sqlParser) {
        if (Strings.isEmpty(sqlParser.getSourceId())) {
            log.warn("data base id is empty");
            return null;
        }
        DatabaseResp databaseResp = getDatabase(Long.parseLong(sqlParser.getSourceId()));
        log.info("database info:{}", databaseResp);
        return queryWithColumns(sqlParser.getSql(), databaseResp);
    }

    private QueryResultWithSchemaResp queryWithColumns(String sql, DatabaseResp databaseResp) {
        QueryResultWithSchemaResp queryResultWithColumns = new QueryResultWithSchemaResp();
        SqlUtils sqlUtils = this.sqlUtils.init(databaseResp);
        log.info("query SQL: {}", sql);
        sqlUtils.queryInternal(sql, queryResultWithColumns);
        return queryResultWithColumns;
    }

    private Optional<DatabaseDO> getDatabaseDO(Long domainId) {
        List<DatabaseDO> databaseDOS = databaseRepository.getDatabaseByDomainId(domainId);
        return databaseDOS.stream().findFirst();
    }

    @Override
    public QueryResultWithSchemaResp getDbNames(Long id) {
        DatabaseResp databaseResp = getDatabase(id);
        EngineAdaptor engineAdaptor = EngineAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        String metaQueryTpl = engineAdaptor.getDbMetaQueryTpl();
        return queryWithColumns(metaQueryTpl, databaseResp);
    }

    @Override
    public QueryResultWithSchemaResp getTables(Long id, String db) {
        DatabaseResp databaseResp = getDatabase(id);
        EngineAdaptor engineAdaptor = EngineAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        String metaQueryTpl = engineAdaptor.getTableMetaQueryTpl();
        String metaQuerySql = String.format(metaQueryTpl, db);
        return queryWithColumns(metaQuerySql, databaseResp);
    }


    @Override
    public QueryResultWithSchemaResp getColumns(Long id, String db, String table) {
        DatabaseResp databaseResp = getDatabase(id);
        EngineAdaptor engineAdaptor = EngineAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        String metaQueryTpl = engineAdaptor.getColumnMetaQueryTpl();
        String metaQuerySql = String.format(metaQueryTpl, db, table);
        return queryWithColumns(metaQuerySql, databaseResp);
    }

}
