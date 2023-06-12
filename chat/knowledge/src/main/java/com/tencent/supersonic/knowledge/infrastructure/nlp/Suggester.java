package com.tencent.supersonic.knowledge.infrastructure.nlp;

import com.hankcs.hanlp.collection.trie.bintrie.BaseNode;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.tencent.supersonic.common.nlp.MapResult;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class Suggester {

    private static final Logger LOGGER = LoggerFactory.getLogger(Suggester.class);
    private static BinTrie<List<String>> trie;
    private static BinTrie<List<String>> suffixTrie;
    private static String localFileCache = "";

    public static final int SEARCH_SIZE = 200;

    static {
        trie = new BinTrie<>();
        suffixTrie = new BinTrie<>();
    }

    /***
     * prefix Search
     * @param key
     * @return
     */
    public static List<MapResult> prefixSearch(String key) {
        return prefixSearch(key, SEARCH_SIZE, trie);
    }

    public static List<MapResult> prefixSearch(String key, int limit) {
        return prefixSearch(key, limit, trie);
    }

    public static List<MapResult> prefixSearch(String key, int limit, BinTrie<List<String>> binTrie) {
        Set<Map.Entry<String, List<String>>> result = prefixSearchLimit(key, limit, binTrie);
        return result.stream().map(
                        entry -> {
                            String name = entry.getKey().replace("#", " ");
                            return new MapResult(name, entry.getValue());
                        }
                ).sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .limit(SEARCH_SIZE)
                .collect(Collectors.toList());
    }

    /***
     * suffix Search
     * @param key
     * @return
     */
    public static List<MapResult> suffixSearch(String key, int limit) {
        String reverseDetectSegment = StringUtils.reverse(key);
        return suffixSearch(reverseDetectSegment, limit, suffixTrie);
    }

    public static List<MapResult> suffixSearch(String key, int limit, BinTrie<List<String>> binTrie) {
        Set<Map.Entry<String, List<String>>> result = prefixSearchLimit(key, limit, binTrie);
        return result.stream().map(
                        entry -> {
                            String name = entry.getKey().replace("#", " ");
                            List<String> natures = entry.getValue().stream()
                                    .map(nature -> nature.replaceAll(NatureType.SUFFIX.getType(), ""))
                                    .collect(Collectors.toList());
                            name = StringUtils.reverse(name);
                            return new MapResult(name, natures);
                        }
                ).sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .limit(SEARCH_SIZE)
                .collect(Collectors.toList());
    }

    private static Set<Map.Entry<String, List<String>>> prefixSearchLimit(String key, int limit,
            BinTrie<List<String>> binTrie) {
        key = key.toLowerCase();
        Set<Map.Entry<String, List<String>>> entrySet = new TreeSet<Map.Entry<String, List<String>>>();
        StringBuilder sb = new StringBuilder(key.substring(0, key.length() - 1));
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
        branch.walkLimit(sb, entrySet, limit);
        return entrySet;
    }

    public static void clear() {
        LOGGER.info("clear all trie");
        trie = new BinTrie<>();
        suffixTrie = new BinTrie<>();
    }

    public static void put(String key, CoreDictionary.Attribute attribute) {
        trie.put(key, Arrays.stream(attribute.nature).map(entry -> entry.toString()).collect(Collectors.toList()));
    }


    public static void loadSuffix(List<WordNature> suffixes) {
        if (CollectionUtils.isEmpty(suffixes)) {
            return;
        }
        TreeMap<String, CoreDictionary.Attribute> map = new TreeMap();
        for (WordNature suffix : suffixes) {
            CoreDictionary.Attribute attributeNew = suffix.getNatureWithFrequency() == null
                    ? new CoreDictionary.Attribute(Nature.nz, 1)
                    : CoreDictionary.Attribute.create(suffix.getNatureWithFrequency());
            if (map.containsKey(suffix.getWord())) {
                attributeNew = DictionaryAttributeUtil.getAttribute(map.get(suffix.getWord()), attributeNew);
            }
            map.put(suffix.getWord(), attributeNew);
        }
        for (Map.Entry<String, CoreDictionary.Attribute> stringAttributeEntry : map.entrySet()) {
            putSuffix(stringAttributeEntry.getKey(), stringAttributeEntry.getValue());
        }
    }

    public static void putSuffix(String key, CoreDictionary.Attribute attribute) {
        suffixTrie.put(key,
                Arrays.stream(attribute.nature).map(entry -> entry.toString()).collect(Collectors.toList()));
    }

}

