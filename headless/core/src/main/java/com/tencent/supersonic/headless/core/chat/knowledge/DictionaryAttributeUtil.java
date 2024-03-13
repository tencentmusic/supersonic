package com.tencent.supersonic.headless.core.chat.knowledge;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Dictionary Attribute Util
 */
public class DictionaryAttributeUtil {

    public static CoreDictionary.Attribute getAttribute(CoreDictionary.Attribute old, CoreDictionary.Attribute add) {
        Map<Nature, Integer> map = new HashMap<>();
        IntStream.range(0, old.nature.length).boxed().forEach(i -> map.put(old.nature[i], old.frequency[i]));
        IntStream.range(0, add.nature.length).boxed().forEach(i -> map.put(add.nature[i], add.frequency[i]));
        List<Map.Entry<Nature, Integer>> list = new LinkedList<Map.Entry<Nature, Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Nature, Integer>>() {
            public int compare(Map.Entry<Nature, Integer> o1, Map.Entry<Nature, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        CoreDictionary.Attribute attribute = new CoreDictionary.Attribute(
                list.stream().map(i -> i.getKey()).collect(Collectors.toList()).toArray(new Nature[0]),
                list.stream().map(i -> i.getValue()).mapToInt(Integer::intValue).toArray(),
                list.stream().map(i -> i.getValue()).findFirst().get());
        if (old.original != null || add.original != null) {
            attribute.original = add.original != null ? add.original : old.original;
        }
        return attribute;
    }
}
