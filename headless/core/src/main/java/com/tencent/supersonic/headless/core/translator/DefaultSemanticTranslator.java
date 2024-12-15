package com.tencent.supersonic.headless.core.translator;

import com.tencent.supersonic.common.calcite.SqlMergeWithUtils;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import com.tencent.supersonic.headless.core.translator.converter.QueryConverter;
import com.tencent.supersonic.headless.core.translator.optimizer.QueryOptimizer;
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

            if (StringUtils.isNotBlank(queryStatement.getSqlQuery().getSimplifiedSql())) {
                queryStatement.setSql(queryStatement.getSqlQuery().getSimplifiedSql());
            }
            if (StringUtils.isBlank(queryStatement.getSql())) {
                throw new RuntimeException("parse exception: " + queryStatement.getErrMsg());
            }

            for (QueryOptimizer queryOptimizer : ComponentFactory.getQueryOptimizers()) {
                queryOptimizer.rewrite(queryStatement);
            }
            log.info("translated query SQL: [{}]", queryStatement.getSql());
        } catch (Exception e) {
            queryStatement.setErrMsg(e.getMessage());
            log.error("Failed to translate query [{}]", e.getMessage(), e);
        }
    }

    private void doOntologyParse(QueryStatement queryStatement) throws Exception {
        OntologyQuery ontologyQuery = queryStatement.getOntologyQuery();
        log.info("parse with ontology: [{}]", ontologyQuery);
        ComponentFactory.getQueryParser().parse(queryStatement);
        if (!queryStatement.isOk()) {
            throw new Exception(String.format("parse ontology table [%s] error [%s]",
                    queryStatement.getSqlQuery().getTable(), queryStatement.getErrMsg()));
        }

        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        String ontologyQuerySql = sqlQuery.getSql();
        String ontologyInnerTable = sqlQuery.getTable();
        String ontologyInnerSql = queryStatement.getSql();

        List<Pair<String, String>> tables = new ArrayList<>();
        tables.add(Pair.of(ontologyInnerTable, ontologyInnerSql));
        if (sqlQuery.isSupportWith()) {
            EngineType engineType = queryStatement.getOntology().getDatabase().getType();
            if (!SqlMergeWithUtils.hasWith(engineType, ontologyQuerySql)) {
                String withSql = "with " + tables.stream()
                        .map(t -> String.format("%s as (%s)", t.getLeft(), t.getRight()))
                        .collect(Collectors.joining(",")) + "\n" + ontologyQuerySql;
                queryStatement.setSql(withSql);
            } else {
                List<String> withTableList =
                        tables.stream().map(Pair::getLeft).collect(Collectors.toList());
                List<String> withSqlList =
                        tables.stream().map(Pair::getRight).collect(Collectors.toList());
                String mergeSql = SqlMergeWithUtils.mergeWith(engineType, ontologyQuerySql,
                        withSqlList, withTableList);
                queryStatement.setSql(mergeSql);
            }
        } else {
            for (Pair<String, String> tb : tables) {
                ontologyQuerySql = StringUtils.replace(ontologyQuerySql, tb.getLeft(),
                        "(" + tb.getRight() + ") " + (sqlQuery.isWithAlias() ? "" : tb.getLeft()),
                        -1);
            }
            queryStatement.setSql(ontologyQuerySql);
        }
    }

}
