package com.tencent.supersonic.chat.query.dsl.optimizer;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.common.util.jsqlparser.CCJSqlParserUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldCorrector extends BaseDSLOptimizer {

    @Override
    public CorrectionInfo rewriter(CorrectionInfo correctionInfo) {
        String replaceFields = CCJSqlParserUtils.replaceFields(correctionInfo.getSql(),
                getFieldToBizName(correctionInfo.getParseInfo().getModelId()));
        correctionInfo.setSql(replaceFields);
        return correctionInfo;
    }
}
