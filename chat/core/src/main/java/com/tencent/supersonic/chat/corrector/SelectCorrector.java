package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        super.correct(semanticCorrectInfo);
        String sql = semanticCorrectInfo.getSql();
        addFieldsToSelect(semanticCorrectInfo, sql);
    }
}
