package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        String preSql = semanticCorrectInfo.getSql();
        semanticCorrectInfo.setPreSql(preSql);
        String sql = SqlParserUpdateHelper.replaceFields(preSql,
                getFieldToBizName(semanticCorrectInfo.getParseInfo().getModelId()));
        semanticCorrectInfo.setSql(sql);
    }
}
