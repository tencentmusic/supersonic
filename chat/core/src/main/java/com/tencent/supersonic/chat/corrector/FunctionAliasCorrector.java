package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FunctionAliasCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        String replaceAlias = SqlParserUpdateHelper.replaceAlias(semanticCorrectInfo.getSql());
        semanticCorrectInfo.setSql(replaceAlias);
    }

}
