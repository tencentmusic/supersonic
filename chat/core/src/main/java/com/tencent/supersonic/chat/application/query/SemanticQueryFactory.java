package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.service.SemanticQuery;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.CollectionUtils;

/**
 * SemanticQueryFactory
 */
public class SemanticQueryFactory {

    private static Map<String, SemanticQuery> strategyFactory = new ConcurrentHashMap<>();

    private static List<SemanticQuery> semanticQueries;


    public static SemanticQuery get(String queryMode) {
        if (CollectionUtils.isEmpty(strategyFactory)) {
            init();
        }

        SemanticQuery semanticQuery = strategyFactory.get(queryMode);
        if (Objects.isNull(semanticQuery)) {
            throw new RuntimeException("not support queryMode :" + queryMode);
        }
        return semanticQuery;
    }

    private static void init() {
        for (SemanticQuery semanticQuery : getSemanticQueries()) {
            strategyFactory.put(semanticQuery.getQueryMode(), semanticQuery);
        }
    }

    public static List<SemanticQuery> getSemanticQueries() {
        if (CollectionUtils.isEmpty(semanticQueries)) {
            semanticQueries = SpringFactoriesLoader.loadFactories(SemanticQuery.class,
                    Thread.currentThread().getContextClassLoader());
        }
        return semanticQueries;
    }
}