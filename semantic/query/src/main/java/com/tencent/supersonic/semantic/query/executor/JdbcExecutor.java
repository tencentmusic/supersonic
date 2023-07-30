package com.tencent.supersonic.semantic.query.executor;

import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.model.domain.utils.SqlUtils;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
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
    public QueryResultWithSchemaResp execute(Catalog catalog, QueryStatement queryStatement) {
        if (Strings.isEmpty(queryStatement.getSourceId())) {
            log.warn("data base id is empty");
            return null;
        }
        log.info("query SQL: {}", queryStatement.getSql());
        DatabaseResp databaseResp = catalog.getDatabase(Long.parseLong(queryStatement.getSourceId()));
        log.info("database info:{}", databaseResp);
        QueryResultWithSchemaResp queryResultWithColumns = new QueryResultWithSchemaResp();
        SqlUtils sqlUtils = this.sqlUtils.init(databaseResp);
        sqlUtils.queryInternal(queryStatement.getSql(), queryResultWithColumns);
        return queryResultWithColumns;
    }

}
