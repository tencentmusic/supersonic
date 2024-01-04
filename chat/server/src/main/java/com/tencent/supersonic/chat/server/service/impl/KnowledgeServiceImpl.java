package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.core.knowledge.DictWord;
import com.tencent.supersonic.chat.core.knowledge.SearchService;
import com.tencent.supersonic.chat.core.utils.HanlpHelper;
import com.tencent.supersonic.chat.server.service.KnowledgeService;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KnowledgeServiceImpl implements KnowledgeService {

    public void updateSemanticKnowledge(List<DictWord> natures) {

        List<DictWord> prefixes = natures.stream()
                .filter(entry -> !entry.getNatureWithFrequency().contains(DictWordType.SUFFIX.getType()))
                .collect(Collectors.toList());

        for (DictWord nature : prefixes) {
            HanlpHelper.addToCustomDictionary(nature);
        }

        List<DictWord> suffixes = natures.stream()
                .filter(entry -> entry.getNatureWithFrequency().contains(DictWordType.SUFFIX.getType()))
                .collect(Collectors.toList());

        SearchService.loadSuffix(suffixes);
    }

    public void reloadAllData(List<DictWord> natures) {
        // 1. reload custom knowledge
        try {
            HanlpHelper.reloadCustomDictionary();
        } catch (Exception e) {
            log.error("reloadCustomDictionary error", e);
        }

        // 2. update online knowledge
        updateOnlineKnowledge(natures);
    }

    public void updateOnlineKnowledge(List<DictWord> natures) {
        try {
            updateSemanticKnowledge(natures);
        } catch (Exception e) {
            log.error("updateSemanticKnowledge error", e);
        }
    }

}
