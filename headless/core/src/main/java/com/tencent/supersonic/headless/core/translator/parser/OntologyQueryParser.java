package com.tencent.supersonic.headless.core.translator.parser;

import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.calcite.SqlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * This parser generates inner sql statement for the ontology query, which would be selected by the
 * parsed sql query.
 */
@Component("OntologyQueryParser")
@Slf4j
public class OntologyQueryParser implements QueryParser {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getOntologyQuery());
    }

    @Override
    public void parse(QueryStatement queryStatement) throws Exception {
        Ontology ontology = queryStatement.getOntology();
        S2CalciteSchema semanticSchema = S2CalciteSchema.builder()
                .schemaKey("DATASET_" + queryStatement.getDataSetId()).ontology(ontology)
                .runtimeOptions(RuntimeOptions.builder().minMaxTime(queryStatement.getMinMaxTime())
                        .enableOptimize(queryStatement.getEnableOptimize()).build())
                .build();
        SqlBuilder sqlBuilder = new SqlBuilder(semanticSchema);
        String sql = sqlBuilder.buildOntologySql(queryStatement);
        queryStatement.getOntologyQuery().setSql(sql);
    }

}
