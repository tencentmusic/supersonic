package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.api.pojo.response.ViewResp;
import com.tencent.supersonic.headless.core.knowledge.DictWord;
import com.tencent.supersonic.headless.core.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.core.knowledge.SearchService;
import com.tencent.supersonic.headless.core.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.KnowledgeService;
import com.tencent.supersonic.headless.server.service.ViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeServiceImpl implements KnowledgeService {

    private final ViewService viewService;

    public KnowledgeServiceImpl(ViewService viewService) {
        this.viewService = viewService;
    }

    @Override
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

    @Override
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

    @Override
    public void updateOnlineKnowledge(List<DictWord> natures) {
        try {
            updateSemanticKnowledge(natures);
        } catch (Exception e) {
            log.error("updateSemanticKnowledge error", e);
        }
    }

    @Override
    public List<S2Term> getTerms(String text) {
        return HanlpHelper.getTerms(text);
    }

    @Override
    public List<HanlpMapResult> prefixSearch(String key, int limit, Set<Long> viewIds) {
        return prefixSearch(key, limit, viewIds2ModelIdList(viewIds));
    }

    public List<HanlpMapResult> prefixSearchByModel(String key, int limit, Set<Long> models) {
        return SearchService.prefixSearch(key, limit, models);
    }

    @Override
    public List<HanlpMapResult> suffixSearch(String key, int limit, Set<Long> viewIds) {
        return suffixSearch(key, limit, viewIds2ModelIdList(viewIds));
    }

    public List<HanlpMapResult> suffixSearchByModel(String key, int limit, Set<Long> models) {
        return SearchService.suffixSearch(key, limit, models);
    }

    private Set<Long> viewIds2ModelIdList(Set<Long> viewIds) {
        Set<Long> modelIds = new HashSet<>();
        MetaFilter filter = new MetaFilter();
        filter.setIds(new ArrayList<>(viewIds));
        List<ViewResp> viewList = viewService.getViewList(filter);
        if (CollectionUtils.isEmpty(viewList)) {
            return modelIds;
        }
        viewList.stream().forEach(view -> modelIds.addAll(view.getAllModels()));
        return modelIds;
    }
}