package com.tencent.supersonic.knowledge.domain.service;

import com.tencent.supersonic.common.nlp.WordNature;
import java.util.List;

/**
 * online knowledge service interface
 */
public interface OnlineKnowledgeService {

    void updateSemanticKnowledge(List<WordNature> natures);

    void reloadAllData(List<WordNature> natures);

    void updateOnlineKnowledge(List<WordNature> natures);

}
