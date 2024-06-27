package com.tencent.supersonic.headless.chat.knowledge;

import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeBaseService {

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

    public List<S2Term> getTerms(String text, Map<Long, List<Long>> modelIdToDataSetIds) {
        return HanlpHelper.getTerms(text, modelIdToDataSetIds);
    }

    public List<HanlpMapResult> prefixSearch(String key, int limit, Map<Long, List<Long>> modelIdToDataSetIds,
            Set<Long> detectDataSetIds) {
        return prefixSearchByModel(key, limit, modelIdToDataSetIds, detectDataSetIds);
    }

    public List<HanlpMapResult> prefixSearchByModel(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        return SearchService.prefixSearch(key, limit, modelIdToDataSetIds, detectDataSetIds);
    }

    public List<HanlpMapResult> suffixSearch(String key, int limit, Map<Long, List<Long>> modelIdToDataSetIds,
            Set<Long> detectDataSetIds) {
        return suffixSearchByModel(key, limit, modelIdToDataSetIds, detectDataSetIds);
    }

    public List<HanlpMapResult> suffixSearchByModel(String key, int limit, Map<Long, List<Long>> modelIdToDataSetIds,
            Set<Long> detectDataSetIds) {
        return SearchService.suffixSearch(key, limit, modelIdToDataSetIds, detectDataSetIds);
    }

}