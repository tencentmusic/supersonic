package com.tencent.supersonic.chat.query.dsl.optimizer;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.common.util.jsqlparser.CCJSqlParserUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TableNameCorrector extends BaseDSLOptimizer {

    public static final String TABLE_PREFIX = "t_";

    @Override
    public CorrectionInfo rewriter(CorrectionInfo correctionInfo) {
        Long modelId = correctionInfo.getParseInfo().getModelId();
        String sqlOutput = correctionInfo.getSql();
        String replaceTable = CCJSqlParserUtils.replaceTable(sqlOutput, TABLE_PREFIX + modelId);
        correctionInfo.setSql(replaceTable);
        return correctionInfo;
    }

}
