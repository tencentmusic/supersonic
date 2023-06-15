package com.tencent.supersonic.chat.application.knowledge;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.chat.domain.pojo.chat.DomainInfos;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import com.tencent.supersonic.knowledge.application.online.WordNatureStrategyFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * word nature service
 **/
@Service
public class WordNatureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordNatureService.class);

    @Autowired
    private SemanticLayer semanticLayer;
    private static final Integer META_CACHE_TIME = 5;

    private List<WordNature> preWordNatures = new ArrayList<>();

    private LoadingCache<String, DomainInfos> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(META_CACHE_TIME, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, DomainInfos>() {
                        @Override
                        public DomainInfos load(String key) {
                            LOGGER.info("load getDomainSchemaInfo cache [{}]", key);
                            return SchemaInfoConverter.convert(semanticLayer.getDomainSchemaInfo(new ArrayList<>()));
                        }
                    }
            );

    public List<WordNature> getAllWordNature() {

        DomainInfos domainInfos = SchemaInfoConverter.convert(semanticLayer.getDomainSchemaInfo(new ArrayList<>()));

        List<WordNature> natures = new ArrayList<>();

        addNatureToResult(NatureType.DIMENSION, domainInfos.getDimensions(), natures);

        addNatureToResult(NatureType.METRIC, domainInfos.getMetrics(), natures);

        addNatureToResult(NatureType.DOMAIN, domainInfos.getDomains(), natures);

        addNatureToResult(NatureType.ENTITY, domainInfos.getEntities(), natures);

        return natures;
    }

    private void addNatureToResult(NatureType value, List<ItemDO> metas, List<WordNature> natures) {
        List<WordNature> natureList = WordNatureStrategyFactory.get(value).getWordNatureList(metas);
        LOGGER.debug("nature type:{} , nature size:{}", value.name(), natureList.size());
        natures.addAll(natureList);
    }

    public List<WordNature> getPreWordNatures() {
        return preWordNatures;
    }

    public void setPreWordNatures(List<WordNature> preWordNatures) {
        this.preWordNatures = preWordNatures;
    }

    public LoadingCache<String, DomainInfos> getCache() {
        return cache;
    }
}
