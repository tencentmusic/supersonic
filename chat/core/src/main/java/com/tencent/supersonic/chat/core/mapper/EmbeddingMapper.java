package com.tencent.supersonic.chat.core.mapper;

import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.core.knowledge.EmbeddingResult;
import com.tencent.supersonic.headless.core.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.core.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.server.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Objects;

/***
 * A mapper that recognizes schema elements with vector embedding.
 */
@Slf4j
public class EmbeddingMapper extends BaseMapper {

    @Override
    public void doMap(QueryContext queryContext) {
        //1. query from embedding by queryText
        String queryText = queryContext.getQueryText();
        KnowledgeService knowledgeService = ContextUtils.getBean(KnowledgeService.class);
        List<S2Term> terms = knowledgeService.getTerms(queryText);

        EmbeddingMatchStrategy matchStrategy = ContextUtils.getBean(EmbeddingMatchStrategy.class);
        List<EmbeddingResult> matchResults = matchStrategy.getMatches(queryContext, terms);

        HanlpHelper.transLetterOriginal(matchResults);

        //2. build SchemaElementMatch by info
        for (EmbeddingResult matchResult : matchResults) {
            Long elementId = Retrieval.getLongId(matchResult.getId());
            Long dataSetId = Retrieval.getLongId(matchResult.getMetadata().get("dataSetId"));
            if (Objects.isNull(dataSetId)) {
                continue;
            }
            SchemaElementType elementType = SchemaElementType.valueOf(matchResult.getMetadata().get("type"));
            SchemaElement schemaElement = getSchemaElement(dataSetId, elementType, elementId,
                    queryContext.getSemanticSchema());
            if (schemaElement == null) {
                continue;
            }
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .element(schemaElement)
                    .frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                    .word(matchResult.getName())
                    .similarity(1 - matchResult.getDistance())
                    .detectWord(matchResult.getDetectWord())
                    .build();
            //3. add to mapInfo
            addToSchemaMap(queryContext.getMapInfo(), dataSetId, schemaElementMatch);
        }
    }
}
