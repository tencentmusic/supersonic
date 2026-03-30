package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.EmbeddingResult;
import com.tencent.supersonic.headless.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import dev.langchain4j.store.embedding.Retrieval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * A mapper that recognizes schema elements with vector embedding.
 */
@Slf4j
public class EmbeddingMapper extends BaseMapper {

    @Override
    public boolean accept(ChatQueryContext chatQueryContext) {
        boolean b0 = MapModeEnum.LOOSE.equals(chatQueryContext.getRequest().getMapModeEnum());
        boolean b1 = chatQueryContext.getRequest().getText2SQLType() == Text2SQLType.LLM_OR_RULE;
        return b0 || b1;
    }

    public void doMap(ChatQueryContext chatQueryContext) {

        // Avoid duplicated embedding retrieval when mapInfo already has matched elements
        // and this stage is entered again in LLM_OR_RULE fallback path.
        if (chatQueryContext.getRequest().getText2SQLType() == Text2SQLType.LLM_OR_RULE
                && !chatQueryContext.getMapInfo().isEmpty()) {
            log.info("skip embedding mapper because mapInfo already contains matched elements");
            return;
        }
        // 1. Query from embedding by queryText
        EmbeddingMatchStrategy matchStrategy = ContextUtils.getBean(EmbeddingMatchStrategy.class);
        List<EmbeddingResult> matchResults = getMatches(chatQueryContext, matchStrategy);

        // Process match results
        HanlpHelper.transLetterOriginal(matchResults);

        // 2. Build SchemaElementMatch based on match results
        for (EmbeddingResult matchResult : matchResults) {
            Long elementId = Retrieval.getLongId(matchResult.getId());
            Long dataSetId = Retrieval.getLongId(matchResult.getMetadata().get("dataSetId"));

            // Skip if dataSetId is null
            if (Objects.isNull(dataSetId)) {
                continue;
            }
            SchemaElementType elementType =
                    SchemaElementType.valueOf(matchResult.getMetadata().get("type"));
            SchemaElement schemaElement = getSchemaElement(dataSetId, elementType, elementId,
                    chatQueryContext.getSemanticSchema());

            // Skip if schemaElement is null
            if (schemaElement == null) {
                continue;
            }


            // Build SchemaElementMatch object
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .element(schemaElement).frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                    .word(matchResult.getName()).similarity(matchResult.getSimilarity())
                    .detectWord(matchResult.getDetectWord()).build();
            schemaElementMatch.setLlmMatched(matchResult.isLlmMatched());

            // 3. Add SchemaElementMatch to mapInfo
            addToSchemaMap(chatQueryContext.getMapInfo(), dataSetId, schemaElementMatch);
        }
        if (CollectionUtils.isEmpty(matchResults)) {
            log.info("embedding mapper no match");
        } else {
            for (EmbeddingResult matchResult : matchResults) {
                log.info("embedding match name=[{}],detectWord=[{}],similarity=[{}],metadata=[{}]",
                        matchResult.getName(), matchResult.getDetectWord(),
                        matchResult.getSimilarity(), JsonUtil.toString(matchResult.getMetadata()));
            }
        }
    }

}
