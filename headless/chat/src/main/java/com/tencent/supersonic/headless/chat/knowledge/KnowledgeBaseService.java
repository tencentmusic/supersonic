package com.tencent.supersonic.headless.chat.knowledge;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeBaseService {
    private static final Map<Long, List<DictWord>> dimValueAliasMap = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Get dimension value alias map (read-only).
     *
     * @return unmodifiable view of the map
     */
    public static Map<Long, List<DictWord>> getDimValueAlias() {
        return Collections.unmodifiableMap(dimValueAliasMap);
    }

    /**
     * Add dimension value aliases with deduplication. Thread-safe implementation using
     * ConcurrentHashMap.
     *
     * @param dimId dimension ID
     * @param newWords new words to add
     * @return updated list of aliases for the dimension
     */
    public static List<DictWord> addDimValueAlias(Long dimId, List<DictWord> newWords) {
        if (dimId == null || CollectionUtils.isEmpty(newWords)) {
            return dimValueAliasMap.get(dimId);
        }

        // Use computeIfAbsent and synchronized block for thread safety
        synchronized (dimValueAliasMap) {
            List<DictWord> dimValueAlias =
                    dimValueAliasMap.computeIfAbsent(dimId, k -> new ArrayList<>());

            // Build deduplication key set
            Set<String> existingKeys = dimValueAlias.stream().map(word -> buildDedupKey(word))
                    .collect(Collectors.toSet());

            // Add new words with deduplication
            for (DictWord dictWord : newWords) {
                String key = buildDedupKey(dictWord);
                if (!existingKeys.contains(key)) {
                    dimValueAlias.add(dictWord);
                    existingKeys.add(key);
                }
            }

            return dimValueAlias;
        }
    }

    /**
     * Remove dimension value aliases by dimension ID.
     *
     * @param dimId dimension ID to remove, or null to clear all
     */
    public static void removeDimValueAlias(Long dimId) {
        if (dimId == null) {
            dimValueAliasMap.clear();
            log.info("Cleared all dimension value aliases");
        } else {
            dimValueAliasMap.remove(dimId);
            log.info("Removed dimension value alias for dimId: {}", dimId);
        }
    }

    /**
     * Build deduplication key for DictWord.
     *
     * @param word the DictWord object
     * @return deduplication key string
     */
    private static String buildDedupKey(DictWord word) {
        return String.format("%s_%s_%s", word.getNatureWithFrequency(), word.getWord(),
                word.getAlias());
    }

    /**
     * Update semantic knowledge (incremental add, no clearing). Use this method to add new words
     * without removing existing data.
     *
     * @param natures the words to add
     */
    public void updateSemanticKnowledge(List<DictWord> natures) {
        lock.writeLock().lock();
        try {
            updateSemanticKnowledgeInternal(natures);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void updateSemanticKnowledgeInternal(List<DictWord> natures) {
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

    /**
     * Reload all knowledge (full replacement with clearing). Use this method to rebuild the entire
     * knowledge base.
     *
     * @param natures all words to load
     */
    public void reloadAllData(List<DictWord> natures) {
        // 1. reload custom knowledge (executed outside lock to avoid long blocking)
        try {
            HanlpHelper.reloadCustomDictionary();
        } catch (Exception e) {
            log.error("reloadCustomDictionary error", e);
        }

        // 2. acquire write lock, clear trie and rebuild (short operation)
        lock.writeLock().lock();
        try {
            SearchService.clear();

            if (CollectionUtils.isNotEmpty(dimValueAliasMap)) {
                for (Long dimId : dimValueAliasMap.keySet()) {
                    natures.addAll(dimValueAliasMap.get(dimId));
                }
            }
            updateSemanticKnowledgeInternal(natures);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<S2Term> getTerms(String text, Map<Long, List<Long>> modelIdToDataSetIds) {
        lock.readLock().lock();
        try {
            return HanlpHelper.getTerms(text, modelIdToDataSetIds);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<HanlpMapResult> prefixSearch(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        lock.readLock().lock();
        try {
            return prefixSearchByModel(key, limit, modelIdToDataSetIds, detectDataSetIds);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<HanlpMapResult> prefixSearchByModel(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        lock.readLock().lock();
        try {
            return SearchService.prefixSearch(key, limit, modelIdToDataSetIds, detectDataSetIds);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<HanlpMapResult> suffixSearch(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        lock.readLock().lock();
        try {
            return suffixSearchByModel(key, limit, modelIdToDataSetIds, detectDataSetIds);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<HanlpMapResult> suffixSearchByModel(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        lock.readLock().lock();
        try {
            return SearchService.suffixSearch(key, limit, modelIdToDataSetIds, detectDataSetIds);
        } finally {
            lock.readLock().unlock();
        }
    }
}
