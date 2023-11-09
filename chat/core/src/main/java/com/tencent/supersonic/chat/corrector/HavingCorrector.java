package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import org.springframework.util.CollectionUtils;

@Slf4j
public class HavingCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryReq queryReq, SemanticParseInfo semanticParseInfo) {

        //add aggregate to all metric
        addHaving(semanticParseInfo);

        //add having expression filed to select
        addHavingToSelect(semanticParseInfo);
    }

    private void addHaving(SemanticParseInfo semanticParseInfo) {
        Long modelId = semanticParseInfo.getModel().getModel();

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        Set<String> metrics = semanticSchema.getMetrics(modelId).stream()
                .map(schemaElement -> schemaElement.getName()).collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(metrics)) {
            return;
        }
        String havingSql = SqlParserAddHelper.addHaving(semanticParseInfo.getSqlInfo().getCorrectS2SQL(), metrics);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(havingSql);
    }

    private void addHavingToSelect(SemanticParseInfo semanticParseInfo) {
        String logicSql = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        if (!SqlParserSelectFunctionHelper.hasAggregateFunction(logicSql)) {
            return;
        }
        Expression havingExpression = SqlParserSelectHelper.getHavingExpression(logicSql);
        if (Objects.nonNull(havingExpression)) {
            String replaceSql = SqlParserAddHelper.addFunctionToSelect(logicSql, havingExpression);
            semanticParseInfo.getSqlInfo().setCorrectS2SQL(replaceSql);
        }
        return;
    }

}
