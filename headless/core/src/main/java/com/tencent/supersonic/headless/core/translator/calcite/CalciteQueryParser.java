package com.tencent.supersonic.headless.core.translator.calcite;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.QueryParser;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Ontology;
import com.tencent.supersonic.headless.core.translator.calcite.sql.RuntimeOptions;
import com.tencent.supersonic.headless.core.translator.calcite.sql.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.calcite.sql.SqlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** the calcite parse implements */
@Component("CalciteQueryParser")
@Slf4j
public class CalciteQueryParser implements QueryParser {

    @Override
    public void parse(QueryStatement queryStatement) throws Exception {
        Ontology ontology = queryStatement.getOntology();
        if (ontology == null) {
            queryStatement.setErrMsg("No ontology could be found");
            return;
        }

        S2CalciteSchema semanticSchema = S2CalciteSchema.builder()
                .schemaKey("DATASET_" + queryStatement.getDataSetId()).ontology(ontology)
                .runtimeOptions(RuntimeOptions.builder().minMaxTime(queryStatement.getMinMaxTime())
                        .enableOptimize(queryStatement.getEnableOptimize()).build())
                .build();
        SqlBuilder sqlBuilder = new SqlBuilder(semanticSchema);
        sqlBuilder.buildOntologySql(queryStatement);
    }

}
