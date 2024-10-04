package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.EmbeddingResult;
import com.tencent.supersonic.headless.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import dev.langchain4j.store.embedding.Retrieval;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/** A mapper that recognizes schema elements with vector embedding. */
@Slf4j
public class EmbeddingMapper extends BaseMapper {

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        // 1. query from embedding by queryText
        EmbeddingMatchStrategy matchStrategy = ContextUtils.getBean(EmbeddingMatchStrategy.class);
        List<EmbeddingResult> matchResults = getMatches(chatQueryContext, matchStrategy);

        HanlpHelper.transLetterOriginal(matchResults);

        // 2. build SchemaElementMatch by info
        for (EmbeddingResult matchResult : matchResults) {
            Long elementId = Retrieval.getLongId(matchResult.getId());
            Long dataSetId = Retrieval.getLongId(matchResult.getMetadata().get("dataSetId"));
            if (Objects.isNull(dataSetId)) {
                continue;
            }
            SchemaElementType elementType =
                    SchemaElementType.valueOf(matchResult.getMetadata().get("type"));
            SchemaElement schemaElement = getSchemaElement(dataSetId, elementType, elementId,
                    chatQueryContext.getSemanticSchema());
            if (schemaElement == null) {
                continue;
            }
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .element(schemaElement).frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                    .word(matchResult.getName()).similarity(matchResult.getSimilarity())
                    .detectWord(matchResult.getDetectWord()).build();
            // 3. add to mapInfo
            addToSchemaMap(chatQueryContext.getMapInfo(), dataSetId, schemaElementMatch);
        }
    }
}
