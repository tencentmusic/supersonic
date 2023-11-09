package com.tencent.supersonic.chat.api.component;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;

/**
 * A semantic corrector checks validity of extracted semantic information and
 * performs correction and optimization if needed.
 */
public interface SemanticCorrector {

    void correct(QueryReq queryReq, SemanticParseInfo semanticParseInfo);
}
