package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.core.knowledge.DictWord;
import java.util.List;

public interface KnowledgeService {

    void updateSemanticKnowledge(List<DictWord> natures);

    void reloadAllData(List<DictWord> natures);

    void updateOnlineKnowledge(List<DictWord> natures);

}
