package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TableNameCorrector extends BaseSemanticCorrector {

    public static final String TABLE_PREFIX = "t_";

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        Long modelId = semanticCorrectInfo.getParseInfo().getModelId();
        String preSql = semanticCorrectInfo.getSql();
        semanticCorrectInfo.setPreSql(preSql);
        String sql = SqlParserUpdateHelper.replaceTable(preSql, TABLE_PREFIX + modelId);
        semanticCorrectInfo.setSql(sql);
    }

}
