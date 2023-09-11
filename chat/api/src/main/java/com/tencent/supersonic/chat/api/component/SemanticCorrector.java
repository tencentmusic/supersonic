package com.tencent.supersonic.chat.api.component;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import net.sf.jsqlparser.JSQLParserException;

/**
 * A semantic corrector checks validity of extracted semantic information and
 * performs correction and optimization if needed.
 */
public interface SemanticCorrector {

    void correct(SemanticCorrectInfo semanticCorrectInfo) throws JSQLParserException;
}
