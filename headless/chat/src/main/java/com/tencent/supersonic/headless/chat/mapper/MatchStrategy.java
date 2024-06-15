package com.tencent.supersonic.headless.chat.mapper;


import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.QueryContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MatchStrategy encapsulates a concrete matching algorithm
 * executed during query or search process.
 */
public interface MatchStrategy<T> {

    Map<MatchText, List<T>> match(QueryContext queryContext, List<S2Term> terms, Set<Long> detectDataSetIds);

}