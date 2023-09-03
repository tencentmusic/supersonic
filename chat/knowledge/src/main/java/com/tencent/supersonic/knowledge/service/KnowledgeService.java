package com.tencent.supersonic.knowledge.service;

import com.tencent.supersonic.knowledge.dictionary.DictWord;

import java.util.List;

public interface KnowledgeService {

    void updateSemanticKnowledge(List<DictWord> natures);

    void reloadAllData(List<DictWord> natures);

    void updateOnlineKnowledge(List<DictWord> natures);

}
