package com.tencent.supersonic.headless.server.web.service.impl;

import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.chat.knowledge.builder.WordBuilderFactory;
import com.tencent.supersonic.headless.server.web.service.SchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DictWordService {

    @Autowired
    private SchemaService schemaService;
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    private List<DictWord> preDictWords = new ArrayList<>();

    public void loadDictWord() {
        List<DictWord> dictWords = getAllDictWords();
        setPreDictWords(dictWords);
        knowledgeBaseService.reloadAllData(dictWords);
    }

    public void reloadDictWord() {
        List<DictWord> dictWords = getAllDictWords();
        List<DictWord> preDictWords = getPreDictWords();
        if (org.apache.commons.collections.CollectionUtils.isEqualCollection(dictWords, preDictWords)) {
            log.debug("dictWords has not changed, reloadKnowledge end");
            return;
        }
        log.info("dictWords has changed");
        setPreDictWords(dictWords);
        knowledgeBaseService.updateOnlineKnowledge(getAllDictWords());
    }

    public List<DictWord> getAllDictWords() {
        SemanticSchema semanticSchema = schemaService.getSemanticSchema();

        List<DictWord> words = new ArrayList<>();

        addWordsByType(DictWordType.DIMENSION, semanticSchema.getDimensions(), words);
        addWordsByType(DictWordType.METRIC, semanticSchema.getMetrics(), words);
        addWordsByType(DictWordType.ENTITY, semanticSchema.getEntities(), words);
        addWordsByType(DictWordType.VALUE, semanticSchema.getDimensionValues(), words);
        addWordsByType(DictWordType.TERM, semanticSchema.getTerms(), words);
        return words;
    }

    private void addWordsByType(DictWordType value, List<SchemaElement> metas, List<DictWord> natures) {
        metas = distinct(metas);
        List<DictWord> natureList = WordBuilderFactory.get(value).getDictWords(metas);
        log.debug("nature type:{} , nature size:{}", value.name(), natureList.size());
        natures.addAll(natureList);
    }

    public List<DictWord> getPreDictWords() {
        return preDictWords;
    }

    public void setPreDictWords(List<DictWord> preDictWords) {
        this.preDictWords = preDictWords;
    }

    private List<SchemaElement> distinct(List<SchemaElement> metas) {
        if (CollectionUtils.isEmpty(metas)) {
            return metas;
        }
        if (SchemaElementType.TERM.equals(metas.get(0).getType())) {
            return metas;
        }
        return metas.stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (e1, e2) -> e1))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

}
