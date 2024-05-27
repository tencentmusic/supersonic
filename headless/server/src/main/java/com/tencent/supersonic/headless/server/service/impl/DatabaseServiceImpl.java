package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Pair;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
        SemanticQueryResp semanticQueryResp = executeSql(sql, databaseResp);
        fillColumnComment(sql, databaseResp, semanticQueryResp);
        return semanticQueryResp;
    }

    @Override
    public SemanticQueryResp executeSql(String sql, DatabaseResp databaseResp) {
        return queryWithColumns(sql, DatabaseConverter.convert(databaseResp));
    }

    private void fillColumnComment(String sql, DatabaseResp databaseResp,
                                   SemanticQueryResp semanticQueryResp) {
        Pair<String, String> dbTableName = getDbTableName(sql, databaseResp);
        String db = dbTableName.first;
        String table = dbTableName.second;
        if (StringUtils.isBlank(db) || StringUtils.isBlank(table)) {
            return;
        }
        SemanticQueryResp columnsWithComment = getColumns(databaseResp, db, table);
        Map<String, String> columnCommentMap = getColumnCommentMap(columnsWithComment.getResultList());
        List<QueryColumn> columns = semanticQueryResp.getColumns();
        for (QueryColumn column : columns) {
            column.setComment(columnCommentMap.get(column.getNameEn()));
        }
    }

    private Map<String, String> getColumnCommentMap(List<Map<String, Object>> resultList) {
        Map<String, String> map = new HashMap<>();
        for (Map<String, Object> result : resultList) {
            map.put(String.valueOf(result.get("name")), String.valueOf(result.get("comment")));
        }
        return map;
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
    public SemanticQueryResp getDbNames(Long id) {
        DatabaseResp databaseResp = getDatabase(id);
        DbAdaptor engineAdaptor = DbAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        String metaQueryTpl = engineAdaptor.getDbMetaQueryTpl();
        return queryWithColumns(metaQueryTpl, DatabaseConverter.convert(databaseResp));
    }

    @Override
    public SemanticQueryResp getTables(Long id, String db) {
        DatabaseResp databaseResp = getDatabase(id);
        DbAdaptor engineAdaptor = DbAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        String metaQueryTpl = engineAdaptor.getTableMetaQueryTpl();
        String metaQuerySql = String.format(metaQueryTpl, db);
        return queryWithColumns(metaQuerySql, DatabaseConverter.convert(databaseResp));
    }

    @Override
    public SemanticQueryResp getColumns(Long id, String db, String table) {
        DatabaseResp databaseResp = getDatabase(id);
        DbAdaptor engineAdaptor = DbAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        String metaQueryTpl = engineAdaptor.getColumnMetaQueryTpl();
        String metaQuerySql = String.format(metaQueryTpl, db, table);
        return queryWithColumns(metaQuerySql, DatabaseConverter.convert(databaseResp));
    }

    public SemanticQueryResp getColumns(DatabaseResp databaseResp, String db, String table) {
        DbAdaptor engineAdaptor = DbAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        String metaQueryTpl = engineAdaptor.getColumnMetaQueryTpl();
        String metaQuerySql = String.format(metaQueryTpl, db, table);
        return queryWithColumns(metaQuerySql, DatabaseConverter.convert(databaseResp));
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
