package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * match strategy
 */
public interface MatchStrategy<T> {

    Map<MatchText, List<T>> match(QueryContext queryContext, List<Term> terms, Set<Long> detectModelId);

}