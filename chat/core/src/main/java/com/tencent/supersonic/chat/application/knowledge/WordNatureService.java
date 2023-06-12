package com.tencent.supersonic.chat.application.knowledge;

import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.chat.domain.pojo.semantic.DomainInfos;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import com.tencent.supersonic.knowledge.application.online.WordNatureStrategyFactory;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * word nature service
 **/
@Service
public class WordNatureService {

    private final Logger logger = LoggerFactory.getLogger(WordNatureService.class);

    @Autowired
    private SemanticLayer semanticLayer;

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
        logger.debug("nature type:{} , nature size:{}", value.name(), natureList.size());
        natures.addAll(natureList);
    }
}
