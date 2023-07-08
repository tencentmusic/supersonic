package com.tencent.supersonic.chat.application.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.domain.pojo.search.MatchText;
import com.tencent.supersonic.common.nlp.MapResult;
import java.util.List;
import java.util.Map;

/**
 * match strategy
 */
public interface MatchStrategy {

    Map<MatchText, List<MapResult>> match(String text, List<Term> terms, Integer detectDomainId);

}