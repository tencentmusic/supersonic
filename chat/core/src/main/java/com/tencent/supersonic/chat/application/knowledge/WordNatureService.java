package com.tencent.supersonic.chat.application.knowledge;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.domain.pojo.chat.DomainInfos;
import com.tencent.supersonic.chat.domain.utils.ComponentFactory;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import com.tencent.supersonic.knowledge.application.online.WordNatureStrategyFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


/**
 * word nature service
 **/
@Service
@Slf4j
public class WordNatureService {

    private static final Integer META_CACHE_TIME = 5;
    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();
    private List<WordNature> preWordNatures = new ArrayList<>();

    private LoadingCache<String, DomainInfos> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(META_CACHE_TIME, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, DomainInfos>() {
                        @Override
                        public DomainInfos load(String key) {
                            log.info("load getDomainSchemaInfo cache [{}]", key);
                            return SchemaInfoConverter.convert(semanticLayer.getDomainSchemaInfo(new ArrayList<>()));
                        }
                    }
            );

    public List<WordNature> getAllWordNature() {
        SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();
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
        log.debug("nature type:{} , nature size:{}", value.name(), natureList.size());
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
