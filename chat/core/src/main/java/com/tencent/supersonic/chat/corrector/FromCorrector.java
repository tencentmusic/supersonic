package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FromCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryReq queryReq, SemanticParseInfo semanticParseInfo) {
        String modelName = semanticParseInfo.getModel().getName();
        SqlParserReplaceHelper.replaceTable(semanticParseInfo.getSqlInfo().getCorrectS2SQL(), modelName);
    }

}
