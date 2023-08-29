package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FunctionCorrector extends BaseSemanticCorrector {

    @Override
    public CorrectionInfo corrector(CorrectionInfo correctionInfo) {
        String replaceFunction = SqlParserUpdateHelper.replaceFunction(correctionInfo.getSql());
        correctionInfo.setSql(replaceFunction);
        return correctionInfo;
    }
}
