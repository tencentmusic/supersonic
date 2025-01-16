package com.tencent.supersonic.headless.core.translator.parser;

import com.tencent.supersonic.headless.api.pojo.enums.ModelDefineType;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlVariableParseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component("SqlVariableParser")
public class SqlVariableParser implements QueryParser {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getStructQuery()) && !queryStatement.getIsS2SQL();
    }

    @Override
    public void parse(QueryStatement queryStatement) {
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchema();
        List<ModelResp> modelResps = semanticSchemaResp.getModelResps();
        if (CollectionUtils.isEmpty(modelResps)) {
            return;
        }
        for (ModelResp modelResp : modelResps) {
            if (ModelDefineType.SQL_QUERY.getName()
                    .equalsIgnoreCase(modelResp.getModelDetail().getQueryType())) {
                String sqlParsed =
                        SqlVariableParseUtils.parse(modelResp.getModelDetail().getSqlQuery(),
                                modelResp.getModelDetail().getSqlVariables(),
                                queryStatement.getStructQuery().getParams());
                ModelResp dataModel =
                        queryStatement.getOntology().getModelMap().get(modelResp.getBizName());
                dataModel.getModelDetail().setSqlQuery(sqlParsed);
            }
        }
    }
}
