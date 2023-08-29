package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * match strategy
 */
public interface MatchStrategy {

    Map<MatchText, List<MapResult>> match(QueryReq queryReq, List<Term> terms, Set<Long> detectModelId);

}