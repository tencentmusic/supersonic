package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Pair;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
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
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.utils.DatabaseConverter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
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
        Database database = DatabaseConverter.convert(databaseReq);
        DatabaseDO databaseDO = getDatabaseDO(databaseReq.getId());
        if (databaseDO != null) {
            database.updatedBy(user.getName());
            DatabaseConverter.convert(database, databaseDO);
            updateById(databaseDO);
            return DatabaseConverter.convertWithPassword(databaseDO);
        }
        database.createdBy(user.getName());
        databaseDO = DatabaseConverter.convert(database);
        save(databaseDO);
        return DatabaseConverter.convertWithPassword(databaseDO);
    }

    @Override
    public List<DatabaseResp> getDatabaseList(User user) {
        List<DatabaseResp> databaseResps = list().stream().map(DatabaseConverter::convert)
                .collect(Collectors.toList());
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
            List<String> datasourceNames = modelResps.stream()
                    .map(ModelResp::getName).collect(Collectors.toList());
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
        sql = SqlVariableParseUtils.parse(sql, sqlExecuteReq.getSqlVariables(), Lists.newArrayList());
        return executeSql(sql, databaseResp);
    }

    @Override
    public SemanticQueryResp executeSql(String sql, DatabaseResp databaseResp) {
        return queryWithColumns(sql, DatabaseConverter.convert(databaseResp));
    }

    private Pair<String, String> getDbTableName(String sql, DatabaseResp databaseResp) {
        String dbTableName = SqlSelectHelper.getDbTableName(sql);
        if (StringUtils.isBlank(dbTableName)) {
            return Pair.pair("", "");
        }
        if (dbTableName.contains(Constants.DOT)) {
            String db = dbTableName.split("\\.")[0];
            String table = dbTableName.split("\\.")[1];
            return Pair.pair(db, table);
        }
        return Pair.pair(databaseResp.getDatabase(), dbTableName);
    }

    @Override
    public Map<String, List<DatabaseParameter>> getDatabaseParameters() {
        return DbParameterFactory.getMap().entrySet().stream().collect(LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue().build()),
                LinkedHashMap::putAll);
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
    public List<DBColumn> getColumns(Long id, String db, String table) throws SQLException {
        DatabaseResp databaseResp = getDatabase(id);
        return getColumns(databaseResp, db, table);
    }

    public List<DBColumn> getColumns(DatabaseResp databaseResp, String db, String table) throws SQLException {
        DbAdaptor engineAdaptor = DbAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        return engineAdaptor.getColumns(DatabaseConverter.getConnectInfo(databaseResp), db, table);
    }

    @Override
    public List<DBColumn> getColumns(Long id, String sql) throws SQLException {
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

    public static void main(String[] args) {
        try {
            String sql = "SELECT * FROM mydatabase.mytable JOIN otherdatabase.othertable ON mytable.id = othertable.id";

            // 解析SQL语句
            Statement statement = CCJSqlParserUtil.parse(sql);

            // 提取库表名
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableNames = tablesNamesFinder.getTableList(statement);

            // 打印库表名
            for (String tableName : tableNames) {
                System.out.println("Table Name: " + tableName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPermission(DatabaseResp databaseResp, User user) {
        List<String> admins = databaseResp.getAdmins();
        List<String> viewers = databaseResp.getViewers();
        if (!admins.contains(user.getName())
                && !viewers.contains(user.getName())
                && !databaseResp.getCreatedBy().equalsIgnoreCase(user.getName())
                && !user.isSuperAdmin()) {
            String message = String.format("您暂无当前数据库%s权限, 请联系数据库创建人:%s开通",
                    databaseResp.getName(),
                    databaseResp.getCreatedBy());
            throw new RuntimeException(message);
        }
    }

}
