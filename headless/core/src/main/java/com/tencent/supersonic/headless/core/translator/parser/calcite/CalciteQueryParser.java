package com.tencent.supersonic.headless.core.translator.parser.calcite;

import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.parser.QueryParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        String sql = sqlBuilder.buildOntologySql(queryStatement);
        queryStatement.setSql(sql);
    }

}
