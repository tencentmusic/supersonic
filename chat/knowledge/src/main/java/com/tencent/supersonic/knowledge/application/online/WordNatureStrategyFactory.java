package com.tencent.supersonic.knowledge.application.online;


import com.tencent.supersonic.common.nlp.NatureType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WordNature Strategy Factory
 */
public class WordNatureStrategyFactory {

    private static Map<NatureType, BaseWordNature> strategyFactory = new ConcurrentHashMap<>();

    static {
        strategyFactory.put(NatureType.DIMENSION, new DimensionWordNature());
        strategyFactory.put(NatureType.METRIC, new MetricWordNature());
        strategyFactory.put(NatureType.DOMAIN, new DomainWordNature());
        strategyFactory.put(NatureType.ENTITY, new EntityWordNature());
        strategyFactory.put(NatureType.VALUE, new ValueWordNature());


    }

    public static BaseWordNature get(NatureType strategyType) {
        return strategyFactory.get(strategyType);
    }
}