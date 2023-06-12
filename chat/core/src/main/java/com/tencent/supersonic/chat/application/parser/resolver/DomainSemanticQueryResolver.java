package com.tencent.supersonic.chat.application.parser.resolver;

import com.tencent.supersonic.chat.api.pojo.SchemaElementCount;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticQuery;
import com.tencent.supersonic.chat.application.query.SemanticQueryFactory;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DomainSemanticQueryResolver implements SemanticQueryResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainSemanticQueryResolver.class);

    @Override
    public SemanticQuery resolve(List<SchemaElementMatch> elementMatches, QueryContextReq queryCtx) {
        Map<SemanticQuery, SchemaElementCount> matchMap = new HashMap<>();

        for (SemanticQuery semanticQuery : SemanticQueryFactory.getSemanticQueries()) {

            SchemaElementCount match = semanticQuery.match(elementMatches, queryCtx);

            if (match != null && match.getCount() > 0 && match.getMaxSimilarity() > 0) {
                LOGGER.info("resolve match [{}:{}] ", semanticQuery.getQueryMode(), match);
                matchMap.put(semanticQuery, match);
            }

        }
        // get the similarity max
        Map.Entry<SemanticQuery, SchemaElementCount> matchMax = matchMap.entrySet().stream()
                .sorted(ContextHelper.SemanticQueryStatComparator).findFirst().orElse(null);
        if (matchMax != null) {
            LOGGER.info("resolve max [{}:{}] ", matchMax.getKey().getQueryMode(), matchMax.getValue());
            return matchMax.getKey();
        }
        return null;
    }
}
