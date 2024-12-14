package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import com.tencent.supersonic.headless.core.pojo.StructQuery;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * This converter converts struct semantic query into sql query by generating S2SQL based on
 * structured semantic information.
 */
@Component("StructQueryConverter")
@Slf4j
public class StructQueryConverter implements QueryConverter {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getStructQuery()) && !queryStatement.getIsS2SQL();
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        StructQuery structQuery = queryStatement.getStructQuery();

        String dsTable = queryStatement.getDataSetName();
        if (Objects.isNull(dsTable)) {
            dsTable = "t_ds_temp";
        }
        SqlQuery sqlQuery = new SqlQuery();
        sqlQuery.setTable(dsTable);
        String sql = String.format("select %s from %s  %s %s %s",
                sqlGenerateUtils.getSelect(structQuery), dsTable,
                sqlGenerateUtils.getGroupBy(structQuery), sqlGenerateUtils.getOrderBy(structQuery),
                sqlGenerateUtils.getLimit(structQuery));
        Database database = queryStatement.getOntology().getDatabase();
        if (!sqlGenerateUtils.isSupportWith(database.getType(), database.getVersion())) {
            sqlQuery.setSupportWith(false);
            sql = String.format("select %s from %s t0 %s %s %s",
                    sqlGenerateUtils.getSelect(structQuery), dsTable,
                    sqlGenerateUtils.getGroupBy(structQuery),
                    sqlGenerateUtils.getOrderBy(structQuery),
                    sqlGenerateUtils.getLimit(structQuery));
        }
        sqlQuery.setSql(sql);
        queryStatement.setSqlQuery(sqlQuery);
        queryStatement.setIsS2SQL(true);

        log.info("parse structQuery [{}] ", queryStatement.getSqlQuery());
    }

}
