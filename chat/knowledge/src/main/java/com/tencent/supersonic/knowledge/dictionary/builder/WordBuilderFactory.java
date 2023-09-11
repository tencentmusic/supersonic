package com.tencent.supersonic.knowledge.dictionary.builder;


import com.tencent.supersonic.common.pojo.enums.DictWordType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DictWord Strategy Factory
 */
public class WordBuilderFactory {

    private static Map<DictWordType, BaseWordBuilder> wordNatures = new ConcurrentHashMap<>();

    static {
        wordNatures.put(DictWordType.DIMENSION, new DimensionWordBuilder());
        wordNatures.put(DictWordType.METRIC, new MetricWordBuilder());
        wordNatures.put(DictWordType.DOMAIN, new ModelWordBuilder());
        wordNatures.put(DictWordType.ENTITY, new EntityWordBuilder());
        wordNatures.put(DictWordType.VALUE, new ValueWordBuilder());
    }

    public static BaseWordBuilder get(DictWordType strategyType) {
        return wordNatures.get(strategyType);
    }
}
