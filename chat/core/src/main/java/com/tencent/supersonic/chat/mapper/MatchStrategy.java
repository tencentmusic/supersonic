package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import java.util.List;
import java.util.Map;

/**
 * match strategy
 */
public interface MatchStrategy {

    Map<MatchText, List<MapResult>> match(String text, List<Term> terms, Long detectModelId);

}