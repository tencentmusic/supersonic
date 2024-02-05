package com.tencent.supersonic.headless.server.service;


import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.core.knowledge.DictWord;
import com.tencent.supersonic.headless.core.knowledge.HanlpMapResult;

import java.util.List;
import java.util.Set;

public interface KnowledgeService {

    List<S2Term> getTerms(String text);

    List<HanlpMapResult> prefixSearch(String key, int limit, Set<Long> viewIds);

    List<HanlpMapResult> suffixSearch(String key, int limit, Set<Long> viewIds);

    void updateSemanticKnowledge(List<DictWord> natures);

    void reloadAllData(List<DictWord> natures);

    void updateOnlineKnowledge(List<DictWord> natures);

}
