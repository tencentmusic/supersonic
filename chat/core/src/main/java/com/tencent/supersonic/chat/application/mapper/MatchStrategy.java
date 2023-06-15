package com.tencent.supersonic.chat.application.mapper;

import com.hankcs.hanlp.algorithm.EditDistance;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.application.knowledge.NatureHelper;
import com.tencent.supersonic.chat.domain.pojo.search.MatchText;
import com.tencent.supersonic.common.nlp.MapResult;
import java.util.List;
import java.util.Map;

/**
 * match strategy
 */
public interface MatchStrategy {

    List<MapResult> match(String text, List<Term> terms, Integer detectDomainId);


    Map<MatchText, List<MapResult>> matchWithMatchText(String text, List<Term> originals, Integer detectDomainId);

    /***
     * exist dimension values
     * @param natures
     * @return
     */
    default boolean existDimensionValues(List<String> natures) {
        for (String nature : natures) {
            if (NatureHelper.isDimensionValueClassId(nature)) {
                return true;
            }
        }
        return false;
    }

    /***
     * get similarity
     * @param detectSegment
     * @param matchName
     * @return
     */
    default double getSimilarity(String detectSegment, String matchName) {
        return 1 - (double) EditDistance.compute(detectSegment, matchName) / Math.max(matchName.length(),
                detectSegment.length());
    }
}