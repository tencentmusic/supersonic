package com.tencent.supersonic.headless.chat.knowledge;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeBaseService {
    private static volatile Map<Long, List<DictWord>> dimValueAliasMap = new HashMap<>();

    public static Map<Long, List<DictWord>> getDimValueAlias() {
        return dimValueAliasMap;
    }

    public static List<DictWord> addDimValueAlias(Long dimId, List<DictWord> newWords) {
        List<DictWord> dimValueAlias =
                dimValueAliasMap.containsKey(dimId) ? dimValueAliasMap.get(dimId)
                        : new ArrayList<>();
        Set<String> wordSet =
                dimValueAlias
                        .stream().map(word -> String.format("%s_%s_%s",
                                word.getNatureWithFrequency(), word.getWord(), word.getAlias()))
                        .collect(Collectors.toSet());
        for (DictWord dictWord : newWords) {
            String key = String.format("%s_%s_%s", dictWord.getNatureWithFrequency(),
                    dictWord.getWord(), dictWord.getAlias());
            if (!wordSet.contains(key)) {
                dimValueAlias.add(dictWord);
            }
        }
        dimValueAliasMap.put(dimId, dimValueAlias);
        return dimValueAlias;
    }

    public void updateSemanticKnowledge(List<DictWord> natures) {

        List<DictWord> prefixes = natures.stream().filter(
                entry -> !entry.getNatureWithFrequency().contains(DictWordType.SUFFIX.getType()))
                .collect(Collectors.toList());

        for (DictWord nature : prefixes) {
            HanlpHelper.addToCustomDictionary(nature);
        }

        List<DictWord> suffixes = natures.stream().filter(
                entry -> entry.getNatureWithFrequency().contains(DictWordType.SUFFIX.getType()))
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
        if (CollectionUtils.isNotEmpty(dimValueAliasMap)) {
            for (Long dimId : dimValueAliasMap.keySet()) {
                natures.addAll(dimValueAliasMap.get(dimId));
            }
        }
        updateOnlineKnowledge(natures);
    }

    private void updateOnlineKnowledge(List<DictWord> natures) {
        try {
            updateSemanticKnowledge(natures);
        } catch (Exception e) {
            log.error("updateSemanticKnowledge error", e);
        }
    }

    public List<S2Term> getTerms(String text, Map<Long, List<Long>> modelIdToDataSetIds) {
        return HanlpHelper.getTerms(text, modelIdToDataSetIds);
    }

    public List<HanlpMapResult> prefixSearch(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        return prefixSearchByModel(key, limit, modelIdToDataSetIds, detectDataSetIds);
    }

    public List<HanlpMapResult> prefixSearchByModel(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        return SearchService.prefixSearch(key, limit, modelIdToDataSetIds, detectDataSetIds);
    }

    public List<HanlpMapResult> suffixSearch(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        return suffixSearchByModel(key, limit, modelIdToDataSetIds, detectDataSetIds);
    }

    public List<HanlpMapResult> suffixSearchByModel(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        return SearchService.suffixSearch(key, limit, modelIdToDataSetIds, detectDataSetIds);
    }
}
