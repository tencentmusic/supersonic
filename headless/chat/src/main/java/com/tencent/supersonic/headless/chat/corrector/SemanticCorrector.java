package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;

/**
 * A semantic corrector checks validity of extracted semantic information and performs correction
 * and optimization if needed.
 */
public interface SemanticCorrector {

    void correct(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo);
}
