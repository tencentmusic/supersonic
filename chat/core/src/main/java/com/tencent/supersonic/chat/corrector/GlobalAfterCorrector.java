package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;

@Slf4j
public class GlobalAfterCorrector extends BaseSemanticCorrector {

    @Override
    public void work(QueryReq queryReq, SemanticParseInfo semanticParseInfo) {

        String logicSql = semanticParseInfo.getSqlInfo().getLogicSql();
        if (!SqlParserSelectFunctionHelper.hasAggregateFunction(logicSql)) {
            return;
        }
        Expression havingExpression = SqlParserSelectHelper.getHavingExpression(logicSql);
        if (Objects.nonNull(havingExpression)) {
            String replaceSql = SqlParserAddHelper.addFunctionToSelect(logicSql, havingExpression);
            semanticParseInfo.getSqlInfo().setLogicSql(replaceSql);
        }
        return;
    }

}