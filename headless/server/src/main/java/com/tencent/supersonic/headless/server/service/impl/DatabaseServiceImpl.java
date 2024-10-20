package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelSchemaReq;
import com.tencent.supersonic.headless.api.pojo.request.SqlExecuteReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptor;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptorFactory;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.utils.JdbcDataSourceUtils;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import com.tencent.supersonic.headless.core.utils.SqlVariableParseUtils;
import com.tencent.supersonic.headless.server.persistence.dataobject.DatabaseDO;
import com.tencent.supersonic.headless.server.persistence.mapper.DatabaseDOMapper;
import com.tencent.supersonic.headless.server.pojo.DatabaseParameter;
import com.tencent.supersonic.headless.server.pojo.DbParameterFactory;
import com.tencent.supersonic.headless.server.pojo.DbParametersBuilder;
import com.tencent.supersonic.headless.server.pojo.DefaultParametersBuilder;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.utils.DatabaseConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DatabaseServiceImpl extends ServiceImpl<DatabaseDOMapper, DatabaseDO>
        implements DatabaseService {

    @Autowired
    private SqlUtils sqlUtils;

    @Lazy
    @Autowired
    private ModelService datasourceService;

    @Override
    public boolean testConnect(DatabaseReq databaseReq, User user) {
        Database database = DatabaseConverter.convert(databaseReq);
        return JdbcDataSourceUtils.testDatabase(database);
    }

    @Override
    public DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user) {
        if (StringUtils.isNotBlank(databaseReq.getDatabaseType())
                && EngineType.OTHER.getName().equalsIgnoreCase(databaseReq.getType())) {
            databaseReq.setType(databaseReq.getDatabaseType());
        }

        DatabaseDO databaseDO = getDatabaseDO(databaseReq.getId());
        if (databaseDO != null) {
            databaseReq.updatedBy(user.getName());
            DatabaseConverter.convert(databaseReq, databaseDO);
            updateById(databaseDO);
            return DatabaseConverter.convertWithPassword(databaseDO);
        }
        databaseReq.createdBy(user.getName());
        databaseDO = DatabaseConverter.convertDO(databaseReq);
        save(databaseDO);
        return DatabaseConverter.convertWithPassword(databaseDO);
    }

    @Override
    public List<DatabaseResp> getDatabaseList(User user) {
        List<DatabaseResp> databaseResps =
                list().stream().map(DatabaseConverter::convert).collect(Collectors.toList());
        fillPermission(databaseResps, user);
        return databaseResps;
    }

    private void fillPermission(List<DatabaseResp> databaseResps, User user) {
        databaseResps.forEach(databaseResp -> {
            if (databaseResp.getAdmins().contains(user.getName())
                    || user.getName().equalsIgnoreCase(databaseResp.getCreatedBy())
                    || user.isSuperAdmin()) {
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
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setDatabaseId(databaseId);
        modelFilter.setIncludesDetail(false);
        List<ModelResp> modelResps = datasourceService.getModelList(modelFilter);
        if (!CollectionUtils.isEmpty(modelResps)) {
            List<String> datasourceNames =
                    modelResps.stream().map(ModelResp::getName).collect(Collectors.toList());
            String message = String.format("该数据库被模型%s使用，无法删除", datasourceNames);
            throw new RuntimeException(message);
        }
        removeById(databaseId);
    }

    @Override
    public DatabaseResp getDatabase(Long id) {
        DatabaseDO databaseDO = getById(id);
        return DatabaseConverter.convertWithPassword(databaseDO);
    }

    @Override
    public DatabaseResp getDatabase(Long id, User user) {
        DatabaseResp databaseResp = getDatabase(id);
        checkPermission(databaseResp, user);
        return databaseResp;
    }

    @Override
    public SemanticQueryResp executeSql(SqlExecuteReq sqlExecuteReq, Long id, User user) {
        DatabaseResp databaseResp = getDatabase(id);
        if (databaseResp == null) {
            return new SemanticQueryResp();
        }
        checkPermission(databaseResp, user);
        String sql = sqlExecuteReq.getSql();
        sql = SqlVariableParseUtils.parse(sql, sqlExecuteReq.getSqlVariables(),
                Lists.newArrayList());
        return executeSql(sql, databaseResp);
    }

    @Override
    public SemanticQueryResp executeSql(String sql, DatabaseResp databaseResp) {
        return queryWithColumns(sql, DatabaseConverter.convert(databaseResp));
    }

    @Override
    public Map<String, List<DatabaseParameter>> getDatabaseParameters(User user) {
        List<DatabaseResp> databaseList = getDatabaseList(user);

        Map<String, DbParametersBuilder> parametersBuilderMap = DbParameterFactory.getMap();
        Map<String, List<DatabaseParameter>> result = new LinkedHashMap<>();

        // Add all known database parameters
        for (Map.Entry<String, DbParametersBuilder> entry : parametersBuilderMap.entrySet()) {
            if (!entry.getKey().equals(EngineType.OTHER.getName())) {
                result.put(entry.getKey(), entry.getValue().build());
            }
        }
        // Add default parameters for unknown databases
        if (!CollectionUtils.isEmpty(databaseList)) {
            List<String> databaseTypeList = databaseList.stream()
                    .map(databaseResp -> databaseResp.getType()).collect(Collectors.toList());
            DefaultParametersBuilder defaultParametersBuilder = new DefaultParametersBuilder();
            for (String dbType : databaseTypeList) {
                if (!parametersBuilderMap.containsKey(dbType)) {
                    result.put(dbType, defaultParametersBuilder.build());
                }
            }
        }
        // Add the OTHER type at the end
        if (parametersBuilderMap.containsKey(EngineType.OTHER.getName())) {
            result.put(EngineType.OTHER.getName(),
                    parametersBuilderMap.get(EngineType.OTHER.getName()).build());
        }
        return result;
    }

    private SemanticQueryResp queryWithColumns(String sql, Database database) {
        SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();
        SqlUtils sqlUtils = this.sqlUtils.init(database);
        log.info("query SQL: {}", sql);
        sqlUtils.queryInternal(sql, queryResultWithColumns);
        return queryResultWithColumns;
    }

    private DatabaseDO getDatabaseDO(Long id) {
        return getById(id);
    }

    @Override
    public List<String> getDbNames(Long id) throws SQLException {
        DatabaseResp databaseResp = getDatabase(id);
        DbAdaptor dbAdaptor = DbAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        return dbAdaptor.getDBs(DatabaseConverter.getConnectInfo(databaseResp));
    }

    @Override
    public List<String> getTables(Long id, String db) throws SQLException {
        DatabaseResp databaseResp = getDatabase(id);
        DbAdaptor dbAdaptor = DbAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        return dbAdaptor.getTables(DatabaseConverter.getConnectInfo(databaseResp), db);
    }

    @Override
    public Map<String, List<DBColumn>> getDbColumns(ModelSchemaReq modelSchemaReq)
            throws SQLException {
        Map<String, List<DBColumn>> dbColumnMap = new HashMap<>();
        if (StringUtils.isNotBlank(modelSchemaReq.getSql())) {
            List<DBColumn> columns =
                    getColumns(modelSchemaReq.getDatabaseId(), modelSchemaReq.getSql());
            dbColumnMap.put(modelSchemaReq.getSql(), columns);
        } else {
            for (String table : modelSchemaReq.getTables()) {
                List<DBColumn> columns =
                        getColumns(modelSchemaReq.getDatabaseId(), modelSchemaReq.getDb(), table);
                dbColumnMap.put(table, columns);
            }
        }
        return dbColumnMap;
    }

    @Override
    public List<DBColumn> getColumns(Long id, String db, String table) throws SQLException {
        DatabaseResp databaseResp = getDatabase(id);
        return getColumns(databaseResp, db, table);
    }

    public List<DBColumn> getColumns(DatabaseResp databaseResp, String db, String table)
            throws SQLException {
        DbAdaptor engineAdaptor = DbAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        return engineAdaptor.getColumns(DatabaseConverter.getConnectInfo(databaseResp), db, table);
    }

    @Override
    public List<DBColumn> getColumns(Long id, String sql) throws SQLException {
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        String wrapSql = String.format("select * from (%s) a limit 1", sql);
        DatabaseResp databaseResp = getDatabase(id);
        SemanticQueryResp semanticQueryResp = executeSql(wrapSql, databaseResp);
        List<DBColumn> dbColumns = Lists.newArrayList();
        for (QueryColumn queryColumn : semanticQueryResp.getColumns()) {
            DBColumn dbColumn = new DBColumn();
            dbColumn.setColumnName(queryColumn.getNameEn());
            dbColumn.setDataType(queryColumn.getType());
            dbColumns.add(dbColumn);
        }
        return dbColumns;
    }

    private void checkPermission(DatabaseResp databaseResp, User user) {
        List<String> admins = databaseResp.getAdmins();
        List<String> viewers = databaseResp.getViewers();
        if (!admins.contains(user.getName()) && !viewers.contains(user.getName())
                && !databaseResp.getCreatedBy().equalsIgnoreCase(user.getName())
                && !user.isSuperAdmin()) {
            String message = String.format("您暂无当前数据库%s权限, 请联系数据库创建人:%s开通", databaseResp.getName(),
                    databaseResp.getCreatedBy());
            throw new RuntimeException(message);
        }
    }
}
