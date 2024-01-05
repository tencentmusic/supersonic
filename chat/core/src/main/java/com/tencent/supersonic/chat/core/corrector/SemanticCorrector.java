package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;

/**
 * A semantic corrector checks validity of extracted semantic information and
 * performs correction and optimization if needed.
 */
public interface SemanticCorrector {

    void correct(QueryContext queryContext, SemanticParseInfo semanticParseInfo);
}
