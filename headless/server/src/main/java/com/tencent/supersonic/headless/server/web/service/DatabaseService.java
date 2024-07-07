package com.tencent.supersonic.headless.server.web.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.request.SqlExecuteReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.pojo.DatabaseParameter;
import java.util.List;
import java.util.Map;


public interface DatabaseService {

    SemanticQueryResp executeSql(String sql, DatabaseResp databaseResp);

    SemanticQueryResp executeSql(SqlExecuteReq sqlExecuteReq, Long id, User user);

    DatabaseResp getDatabase(Long id, User user);

    DatabaseResp getDatabase(Long id);

    Map<String, List<DatabaseParameter>> getDatabaseParameters();

    boolean testConnect(DatabaseReq databaseReq, User user);

    DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user);

    List<DatabaseResp> getDatabaseList(User user);

    void deleteDatabase(Long databaseId);

    SemanticQueryResp getDbNames(Long id);

    SemanticQueryResp getTables(Long id, String db);

    SemanticQueryResp getColumns(Long id, String db, String table);
}
