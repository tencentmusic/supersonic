package com.tencent.supersonic.chat.application.parser.resolver;

import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticQuery;
import java.util.List;

/**
 * Base interface for resolving query mode.
 */
public interface SemanticQueryResolver {

    SemanticQuery resolve(List<SchemaElementMatch> elementMatches, QueryContextReq queryCtx);
}
