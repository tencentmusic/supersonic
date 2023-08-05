package com.tencent.supersonic.chat.query;

import com.tencent.supersonic.chat.api.component.SemanticQuery;

import java.util.List;

/**
 * This interface defines the contract for a selector that picks the most suitable semantic query.
 **/
public interface QuerySelector {

    List<SemanticQuery> select(List<SemanticQuery> candidateQueries);
}
