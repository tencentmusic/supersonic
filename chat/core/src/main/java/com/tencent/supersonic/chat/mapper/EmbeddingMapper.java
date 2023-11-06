package com.tencent.supersonic.chat.mapper;

import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.knowledge.dictionary.EmbeddingResult;
import com.tencent.supersonic.knowledge.dictionary.builder.BaseWordBuilder;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/***
 * A mapper that is capable of semantic understanding of text.
 */
@Slf4j
public class EmbeddingMapper extends BaseMapper {

    @Override
    public void work(QueryContext queryContext) {
        //1. query from embedding by queryText

        String queryText = queryContext.getRequest().getQueryText();
        List<Term> terms = HanlpHelper.getTerms(queryText);

        EmbeddingMatchStrategy matchStrategy = ContextUtils.getBean(EmbeddingMatchStrategy.class);
        List<EmbeddingResult> matchResults = matchStrategy.getMatches(queryContext, terms);

        HanlpHelper.transLetterOriginal(matchResults);

        //2. build SchemaElementMatch by info
        MapperHelper mapperHelper = ContextUtils.getBean(MapperHelper.class);
        for (EmbeddingResult matchResult : matchResults) {
            Long elementId = Retrieval.getLongId(matchResult.getId());

            SchemaElement schemaElement = JSONObject.parseObject(JSONObject.toJSONString(matchResult.getMetadata()),
                    SchemaElement.class);

            String modelIdStr = matchResult.getMetadata().get("modelId");
            if (StringUtils.isBlank(modelIdStr)) {
                continue;
            }
            long modelId = Long.parseLong(modelIdStr);

            schemaElement = getSchemaElement(modelId, schemaElement.getType(), elementId);

            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .element(schemaElement)
                    .frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                    .word(matchResult.getName())
                    .similarity(mapperHelper.getSimilarity(matchResult.getName(), matchResult.getDetectWord()))
                    .detectWord(matchResult.getDetectWord())
                    .build();
            //3. add to mapInfo
            addToSchemaMap(queryContext.getMapInfo(), modelId, schemaElementMatch);
        }
    }
}
