package com.tencent.supersonic.semantic.model.application;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter.EngineAdaptor;
import com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter.EngineAdaptorFactory;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatabaseDO;
import com.tencent.supersonic.semantic.model.domain.repository.DatabaseRepository;
import com.tencent.supersonic.semantic.model.domain.utils.DatabaseConverter;
import com.tencent.supersonic.semantic.model.domain.utils.JdbcDataSourceUtils;
import com.tencent.supersonic.semantic.model.domain.utils.SqlUtils;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.pojo.Database;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class DatabaseServiceImpl implements DatabaseService {

    private final SqlUtils sqlUtils;
    private DatabaseRepository databaseRepository;
    private DatasourceService datasourceService;

    public DatabaseServiceImpl(DatabaseRepository databaseRepository,
                               SqlUtils sqlUtils,
                               @Lazy DatasourceService datasourceService) {
        this.databaseRepository = databaseRepository;
        this.sqlUtils = sqlUtils;
        this.datasourceService = datasourceService;
    }

    @Override
    public boolean testConnect(DatabaseReq databaseReq, User user) {
        Database database = DatabaseConverter.convert(databaseReq, user);
        return JdbcDataSourceUtils.testDatabase(database);
    }

    @Override
    public DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user) {
        Database database = DatabaseConverter.convert(databaseReq, user);
        DatabaseDO databaseDO = getDatabaseDO(databaseReq.getId());
        if (databaseDO != null) {
            DatabaseConverter.convert(database, databaseDO);
            databaseRepository.updateDatabase(databaseDO);
            return DatabaseConverter.convert(databaseDO);
        }
        databaseDO = DatabaseConverter.convert(database);
        databaseRepository.createDatabase(databaseDO);
        return DatabaseConverter.convert(databaseDO);
    }

    @Override
    public List<DatabaseResp> getDatabaseList(User user) {
        List<DatabaseResp> databaseResps =
                databaseRepository.getDatabaseList()
                .stream().map(DatabaseConverter::convert)
                .collect(Collectors.toList());
        fillPermission(databaseResps, user);
        return databaseResps;
    }

    private void fillPermission(List<DatabaseResp> databaseResps, User user) {
        databaseResps.forEach(databaseResp -> {
            if (databaseResp.getAdmins().contains(user.getName())
                    || user.getName().equalsIgnoreCase(databaseResp.getCreatedBy())) {
                databaseResp.setHasPermission(true);
                databaseResp.setHasEditPermission(true);
                databaseResp.setHasUsePermission(true);
            }
            if (databaseResp.getViewers().contains(user.getName())) {
                databaseResp.setHasUsePermission(true);
            }
        });
    }

    @Override
    public void deleteDatabase(Long databaseId) {
        List<DatasourceResp> datasourceResps = datasourceService.getDatasourceList(databaseId);
        if (!CollectionUtils.isEmpty(datasourceResps)) {
            List<String> datasourceNames = datasourceResps.stream()
                    .map(DatasourceResp::getName).collect(Collectors.toList());
            String message = String.format("该数据库被数据源%s使用，无法删除", datasourceNames);
            throw new RuntimeException(message);
        }
        databaseRepository.deleteDatabase(databaseId);
    }

    @Override
    public DatabaseResp getDatabase(Long id) {
        DatabaseDO databaseDO = databaseRepository.getDatabase(id);
        return DatabaseConverter.convert(databaseDO);
    }

    @Override
    public QueryResultWithSchemaResp executeSql(String sql, Long id, User user) {
        DatabaseResp databaseResp = getDatabase(id);
        if (databaseResp == null) {
            return new QueryResultWithSchemaResp();
        }
        List<String> admins = databaseResp.getAdmins();
        List<String> viewers = databaseResp.getViewers();
        if (!admins.contains(user.getName())
                && !viewers.contains(user.getName())
                && !databaseResp.getCreatedBy().equalsIgnoreCase(user.getName())) {
            String message = String.format("您暂无当前数据库%s权限, 请联系数据库管理员%s开通",
                    databaseResp.getName(),
                    String.join(",", admins));
            throw new RuntimeException(message);
        }
        return executeSql(sql, databaseResp);
    }

    @Override
    public QueryResultWithSchemaResp executeSql(String sql, DatabaseResp databaseResp) {
        return queryWithColumns(sql, databaseResp);
    }

    private QueryResultWithSchemaResp queryWithColumns(String sql, DatabaseResp databaseResp) {
        QueryResultWithSchemaResp queryResultWithColumns = new QueryResultWithSchemaResp();
        SqlUtils sqlUtils = this.sqlUtils.init(databaseResp);
        log.info("query SQL: {}", sql);
        sqlUtils.queryInternal(sql, queryResultWithColumns);
        return queryResultWithColumns;
    }

    private DatabaseDO getDatabaseDO(Long id) {
        return databaseRepository.getDatabase(id);
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
