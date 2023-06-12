package com.tencent.supersonic.knowledge.application.online;

import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import com.tencent.supersonic.knowledge.domain.service.OnlineKnowledgeService;
import com.tencent.supersonic.knowledge.infrastructure.nlp.HanlpHelper;
import com.tencent.supersonic.knowledge.infrastructure.nlp.Suggester;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * online knowledge service impl
 */
@Service
public class OnlineKnowledgeServiceImpl implements OnlineKnowledgeService {

    private final Logger logger = LoggerFactory.getLogger(OnlineKnowledgeServiceImpl.class);

    public void updateSemanticKnowledge(List<WordNature> natures) {

        List<WordNature> prefixes = natures.stream()
                .filter(entry -> !entry.getNatureWithFrequency().contains(NatureType.SUFFIX.getType()))
                .collect(Collectors.toList());

        for (WordNature nature : prefixes) {
            HanlpHelper.addToCustomDictionary(nature);
        }

        List<WordNature> suffixes = natures.stream()
                .filter(entry -> entry.getNatureWithFrequency().contains(NatureType.SUFFIX.getType()))
                .collect(Collectors.toList());

        Suggester.loadSuffix(suffixes);
    }


    public void reloadAllData(List<WordNature> natures) {
        // 1. reload custom knowledge
        try {
            HanlpHelper.reloadCustomDictionary();
        } catch (Exception e) {
            logger.error("reloadCustomDictionary error", e);
        }

        // 2. update online knowledge
        updateOnlineKnowledge(natures);
    }

    public void updateOnlineKnowledge(List<WordNature> natures) {
        try {
            updateSemanticKnowledge(natures);
        } catch (Exception e) {
            logger.error("updateSemanticKnowledge error", e);
        }
    }

}