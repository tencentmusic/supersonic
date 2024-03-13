package com.tencent.supersonic.headless.core.chat.corrector;


import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.core.pojo.QueryContext;

/**
 * A semantic corrector checks validity of extracted semantic information and
 * performs correction and optimization if needed.
 */
public interface SemanticCorrector {

    void correct(QueryContext queryContext, SemanticParseInfo semanticParseInfo);
}
