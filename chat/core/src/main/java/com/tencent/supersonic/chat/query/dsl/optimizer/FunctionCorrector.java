package com.tencent.supersonic.chat.query.dsl.optimizer;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.common.util.jsqlparser.CCJSqlParserUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FunctionCorrector extends BaseDSLOptimizer {

    @Override
    public CorrectionInfo rewriter(CorrectionInfo correctionInfo) {
        String replaceFunction = CCJSqlParserUtils.replaceFunction(correctionInfo.getSql());
        correctionInfo.setSql(replaceFunction);
        return correctionInfo;
    }
}
