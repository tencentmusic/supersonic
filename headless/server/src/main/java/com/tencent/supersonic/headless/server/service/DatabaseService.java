package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import com.tencent.supersonic.headless.api.pojo.request.SqlExecuteReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.pojo.DatabaseParameter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DatabaseService {

    SemanticQueryResp executeSql(String sql, DatabaseResp databaseResp);

    List<DatabaseResp> getDatabaseByType(DataType dataType);

    SemanticQueryResp executeSql(SqlExecuteReq sqlExecuteReq, User user);

    DatabaseResp getDatabase(Long id, User user);

    DatabaseResp getDatabase(Long id);

    Map<String, List<DatabaseParameter>> getDatabaseParameters(User user);

    boolean testConnect(DatabaseReq databaseReq, User user);

    DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user);

    List<DatabaseResp> getDatabaseList(User user);

    void deleteDatabase(Long databaseId);

    List<String> getCatalogs(Long id) throws SQLException;

    List<String> getDbNames(Long id, String catalog) throws SQLException;

    List<String> getTables(Long id, String catalog, String db) throws SQLException;

    Map<String, List<DBColumn>> getDbColumns(ModelBuildReq modelBuildReq) throws SQLException;

    List<DBColumn> getColumns(Long id, String catalog, String db, String table) throws SQLException;

    List<DBColumn> getColumns(Long id, String sql) throws SQLException;
}
