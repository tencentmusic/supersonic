package com.tencent.supersonic.headless.chat.knowledge;

import com.hankcs.hanlp.collection.trie.bintrie.BaseNode;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
import com.tencent.supersonic.headless.chat.utils.EditDistanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
public class SearchService {

    public static final int SEARCH_SIZE = 200;
    private static BinTrie<List<String>> trie;
    private static BinTrie<List<String>> suffixTrie;

    static {
        trie = new BinTrie<>();
        suffixTrie = new BinTrie<>();
    }

    /**
     * * prefix Search
     *
     * @param key
     * @return
     */
    public static List<HanlpMapResult> prefixSearch(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        return prefixSearch(key, limit, trie, modelIdToDataSetIds, detectDataSetIds);
    }

    public static List<HanlpMapResult> prefixSearch(String key, int limit,
            BinTrie<List<String>> binTrie, Map<Long, List<Long>> modelIdToDataSetIds,
            Set<Long> detectDataSetIds) {
        Set<Long> modelIdOrDataSetIds =
                findModelIdOrDataSetIds(modelIdToDataSetIds, detectDataSetIds);
        Set<Map.Entry<String, List<String>>> result = search(key, binTrie, modelIdOrDataSetIds);
        List<HanlpMapResult> hanlpMapResults = result.stream().map(entry -> {
            String name = entry.getKey().replace("#", " ");
            double similarity = EditDistanceUtils.getSimilarity(name, key);
            return new HanlpMapResult(name, entry.getValue(), key, similarity);
        }).sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .collect(Collectors.toList());
        return transformAndFilterByDataSet(hanlpMapResults, modelIdToDataSetIds, detectDataSetIds,
                limit);
    }

    /**
     * * suffix Search
     *
     * @param key
     * @return
     */
    public static List<HanlpMapResult> suffixSearch(String key, int limit,
            Map<Long, List<Long>> modelIdToDataSetIds, Set<Long> detectDataSetIds) {
        return suffixSearch(key, limit, suffixTrie, modelIdToDataSetIds, detectDataSetIds);
    }

    public static List<HanlpMapResult> suffixSearch(String key, int limit,
            BinTrie<List<String>> binTrie, Map<Long, List<Long>> modelIdToDataSetIds,
            Set<Long> detectDataSetIds) {
        String reverseDetectSegment = StringUtils.reverse(key);
        Set<Long> modelIdOrDataSetIds =
                findModelIdOrDataSetIds(modelIdToDataSetIds, detectDataSetIds);

        Set<Map.Entry<String, List<String>>> result =
                search(reverseDetectSegment, binTrie, modelIdOrDataSetIds);
        List<HanlpMapResult> hanlpMapResults = result.stream().map(entry -> {
            String name = entry.getKey().replace("#", " ");
            List<String> natures = entry.getValue().stream()
                    .map(nature -> nature.replaceAll(DictWordType.SUFFIX.getType(), ""))
                    .collect(Collectors.toList());

            name = StringUtils.reverse(name);
            double similarity = EditDistanceUtils.getSimilarity(name, key);
            return new HanlpMapResult(name, natures, key, similarity);
        }).sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .collect(Collectors.toList());
        return transformAndFilterByDataSet(hanlpMapResults, modelIdToDataSetIds, detectDataSetIds,
                limit);
    }

    private static List<HanlpMapResult> transformAndFilterByDataSet(
            List<HanlpMapResult> hanlpMapResults, Map<Long, List<Long>> modelIdToDataSetIds,
            Set<Long> detectDataSetIds, int limit) {
        return hanlpMapResults.stream().peek(hanlpMapResult -> {
            List<String> natures = hanlpMapResult.getNatures().stream()
                    .map(nature -> NatureHelper.changeModel2DataSet(nature, modelIdToDataSetIds))
                    .flatMap(Collection::stream).filter(nature -> {
                        if (CollectionUtils.isEmpty(detectDataSetIds)) {
                            return true;
                        }
                        Long dataSetId = NatureHelper.getDataSetId(nature);
                        if (dataSetId != null) {
                            return detectDataSetIds.contains(dataSetId);
                        }
                        return false;
                    }).collect(Collectors.toList());
            hanlpMapResult.setNatures(natures);
        }).filter(hanlpMapResult -> !CollectionUtils.isEmpty(hanlpMapResult.getNatures()))
                .limit(limit).collect(Collectors.toList());
    }

    private static Set<Map.Entry<String, List<String>>> search(String key,
            BinTrie<List<String>> binTrie, Set<Long> modelIdOrDataSetIds) {
        key = key.toLowerCase();
        Set<Map.Entry<String, List<String>>> entrySet =
                new TreeSet<Map.Entry<String, List<String>>>();

        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(key)) {
            sb = new StringBuilder(key.substring(0, key.length() - 1));
        }
        BaseNode branch = binTrie;
        char[] chars = key.toCharArray();
        for (char aChar : chars) {
            if (branch == null) {
                return entrySet;
            }
            branch = branch.getChild(aChar);
        }

        if (branch == null) {
            return entrySet;
        }
        branch.walkLimit(sb, entrySet, modelIdOrDataSetIds);
        return entrySet;
    }

    public static void clear() {
        log.debug("clear all trie");
        trie = new BinTrie<>();
        suffixTrie = new BinTrie<>();
    }

    public static void put(String key, CoreDictionary.Attribute attribute) {
        trie.put(key, getValue(attribute.nature));
    }

    public static void loadSuffix(List<DictWord> suffixes) {
        if (CollectionUtils.isEmpty(suffixes)) {
            return;
        }
        TreeMap<String, CoreDictionary.Attribute> map = new TreeMap();
        for (DictWord suffix : suffixes) {
            CoreDictionary.Attribute attributeNew = suffix.getNatureWithFrequency() == null
                    ? new CoreDictionary.Attribute(Nature.nz, 1)
                    : CoreDictionary.Attribute.create(suffix.getNatureWithFrequency());
            if (map.containsKey(suffix.getWord())) {
                attributeNew = DictionaryAttributeUtil.getAttribute(map.get(suffix.getWord()),
                        attributeNew);
            }
            map.put(suffix.getWord(), attributeNew);
        }
        for (Map.Entry<String, CoreDictionary.Attribute> stringAttributeEntry : map.entrySet()) {
            putSuffix(stringAttributeEntry.getKey(), stringAttributeEntry.getValue());
        }
    }

    public static void putSuffix(String key, CoreDictionary.Attribute attribute) {
        Nature[] nature = attribute.nature;
        suffixTrie.put(key, getValue(nature));
    }

    private static List<String> getValue(Nature[] nature) {
        return Arrays.stream(nature).map(entry -> entry.toString()).collect(Collectors.toList());
    }

    public static void remove(DictWord dictWord, Nature[] natures) {
        trie.remove(dictWord.getWord());
        if (Objects.nonNull(natures) && natures.length > 0) {
            trie.put(dictWord.getWord(), getValue(natures));
        }
        if (dictWord.getNature().contains(DictWordType.METRIC.getType())
                || dictWord.getNature().contains(DictWordType.DIMENSION.getType())) {
            suffixTrie.remove(dictWord.getWord());
        }
    }

    public static List<String> getDimensionValue(DimensionValueReq dimensionValueReq) {
        String nature = DictWordType.NATURE_SPILT + dimensionValueReq.getModelId()
                + DictWordType.NATURE_SPILT + dimensionValueReq.getElementID();
        PriorityQueue<Term> terms = MultiCustomDictionary.NATURE_TO_VALUES.get(nature);
        if (CollectionUtils.isEmpty(terms)) {
            return new ArrayList<>();
        }
        return terms.stream().map(term -> term.getWord()).collect(Collectors.toList());
    }

    /**
     * Find all modelIds and dataSetIds based on the dataSetId
     */
    public static Set<Long> findModelIdOrDataSetIds(Map<Long, List<Long>> modelIdToDataSetIds,
            Set<Long> detectDataSetIds) {
        if (CollectionUtils.isEmpty(detectDataSetIds)) {
            return new HashSet<>();
        }
        if (CollectionUtils.isEmpty(modelIdToDataSetIds)) {
            return new HashSet<>(detectDataSetIds);
        }
        Set<Long> result = modelIdToDataSetIds.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(detectDataSetIds::contains))
                .map(Map.Entry::getKey).collect(Collectors.toSet());

        result.addAll(detectDataSetIds);
        return result;
    }
}
