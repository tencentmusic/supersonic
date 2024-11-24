package com.tencent.supersonic.headless.core.translator;

import com.tencent.supersonic.common.calcite.SqlMergeWithUtils;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQueryParam;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.OntologyQueryParam;
import com.tencent.supersonic.headless.core.translator.converter.QueryConverter;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DefaultSemanticTranslator implements SemanticTranslator {

    public void translate(QueryStatement queryStatement) {
        if (queryStatement.isTranslated()) {
            return;
        }
        try {
            for (QueryConverter converter : ComponentFactory.getQueryConverters()) {
                if (converter.accept(queryStatement)) {
                    log.debug("QueryConverter accept [{}]", converter.getClass().getName());
                    converter.convert(queryStatement);
                }
            }
            doOntologyParse(queryStatement);

            if (StringUtils.isNotBlank(queryStatement.getSqlQueryParam().getSimplifiedSql())) {
                queryStatement.setSql(queryStatement.getSqlQueryParam().getSimplifiedSql());
            }
            if (StringUtils.isBlank(queryStatement.getSql())) {
                throw new RuntimeException("parse exception: " + queryStatement.getErrMsg());
            }
            if (!SqlSelectHelper.hasLimit(queryStatement.getSql())) {
                queryStatement
                        .setSql(queryStatement.getSql() + " limit " + queryStatement.getLimit());
            }

            for (QueryOptimizer queryOptimizer : ComponentFactory.getQueryOptimizers()) {
                queryOptimizer.rewrite(queryStatement);
            }
        } catch (Exception e) {
            queryStatement.setErrMsg(e.getMessage());
            log.error("Failed to translate semantic query [{}]", e);
        }
    }

    private void doOntologyParse(QueryStatement queryStatement) throws Exception {
        OntologyQueryParam ontologyQueryParam = queryStatement.getOntologyQueryParam();
        SqlQueryParam sqlQueryParam = queryStatement.getSqlQueryParam();
        log.info("parse with ontology: [{}]", ontologyQueryParam);
        ComponentFactory.getQueryParser().parse(queryStatement);

        if (!queryStatement.isOk()) {
            throw new Exception(String.format("parse table [%s] error [%s]",
                    sqlQueryParam.getTable(), queryStatement.getErrMsg()));
        }

        List<Pair<String, String>> tables = new ArrayList<>();
        tables.add(Pair.of(sqlQueryParam.getTable(), queryStatement.getSql()));
        if (sqlQueryParam.isSupportWith()) {
            EngineType engineType =
                    EngineType.fromString(queryStatement.getOntology().getDatabase().getType());
            if (!SqlMergeWithUtils.hasWith(engineType, sqlQueryParam.getSql())) {
                String withSql = "with " + tables.stream()
                        .map(t -> String.format("%s as (%s)", t.getLeft(), t.getRight()))
                        .collect(Collectors.joining(",")) + "\n" + sqlQueryParam.getSql();
                queryStatement.setSql(withSql);
            } else {
                List<String> parentTableList =
                        tables.stream().map(Pair::getLeft).collect(Collectors.toList());
                List<String> parentSqlList =
                        tables.stream().map(Pair::getRight).collect(Collectors.toList());
                String mergeSql = SqlMergeWithUtils.mergeWith(engineType, sqlQueryParam.getSql(),
                        parentSqlList, parentTableList);
                queryStatement.setSql(mergeSql);
            }
        } else {
            String dsSql = sqlQueryParam.getSql();
            for (Pair<String, String> tb : tables) {
                dsSql = StringUtils.replace(dsSql, tb.getLeft(), "(" + tb.getRight() + ") "
                        + (sqlQueryParam.isWithAlias() ? "" : tb.getLeft()), -1);
            }
            queryStatement.setSql(dsSql);
        }
    }

}
