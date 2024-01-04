package com.tencent.supersonic.chat.core.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MatchStrategy encapsulates a concrete matching algorithm
 * executed during query or search process.
 */
public interface MatchStrategy<T> {

    Map<MatchText, List<T>> match(QueryContext queryContext, List<Term> terms, Set<Long> detectModelId);

}