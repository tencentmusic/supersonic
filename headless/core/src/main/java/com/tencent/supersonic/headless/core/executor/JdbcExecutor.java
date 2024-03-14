package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component("JdbcExecutor")
@Slf4j
public class JdbcExecutor implements QueryExecutor {
    @Override
    public boolean accept(QueryStatement queryStatement) {
        return true;
    }

    @Override
    public SemanticQueryResp execute(QueryStatement queryStatement) {
        SqlUtils sqlUtils = ContextUtils.getBean(SqlUtils.class);
        if (Strings.isEmpty(queryStatement.getSourceId())) {
            log.warn("data base id is empty");
            return null;
        }
        log.info("query SQL: {}", queryStatement.getSql());
        Database database = queryStatement.getSemanticModel().getDatabase();
        SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();
        SqlUtils sqlUtil = sqlUtils.init(database);
        sqlUtil.queryInternal(queryStatement.getSql(), queryResultWithColumns);
        queryResultWithColumns.setSql(queryStatement.getSql());
        return queryResultWithColumns;
    }

}
