package com.tencent.supersonic.chat.api.component;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import net.sf.jsqlparser.JSQLParserException;

public interface DSLOptimizer {
    CorrectionInfo rewriter(CorrectionInfo correctionInfo) throws JSQLParserException;

}
