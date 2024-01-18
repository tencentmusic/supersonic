package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component("JdbcExecutor")
@Slf4j
public class JdbcExecutor implements QueryExecutor {

    private final SqlUtils sqlUtils;

    public JdbcExecutor(SqlUtils sqlUtils) {
        this.sqlUtils = sqlUtils;
    }

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return true;
    }

    @Override
    public QueryResultWithSchemaResp execute(QueryStatement queryStatement) {
        if (Strings.isEmpty(queryStatement.getSourceId())) {
            log.warn("data base id is empty");
            return null;
        }
        log.info("query SQL: {}", queryStatement.getSql());
        Database database = queryStatement.getSemanticModel().getDatabase();
        log.info("database info:{}", database);
        QueryResultWithSchemaResp queryResultWithColumns = new QueryResultWithSchemaResp();
        SqlUtils sqlUtils = this.sqlUtils.init(database);
        sqlUtils.queryInternal(queryStatement.getSql(), queryResultWithColumns);
        return queryResultWithColumns;
    }

}
