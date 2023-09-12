package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldCorrector extends BaseSemanticCorrector {

    @Override
    public CorrectionInfo corrector(CorrectionInfo correctionInfo) {
        String preSql = correctionInfo.getSql();
        correctionInfo.setPreSql(preSql);
        String sql = SqlParserUpdateHelper.replaceFields(preSql,
                getFieldToBizName(correctionInfo.getParseInfo().getModelId()));
        correctionInfo.setSql(sql);
        return correctionInfo;
    }
}
