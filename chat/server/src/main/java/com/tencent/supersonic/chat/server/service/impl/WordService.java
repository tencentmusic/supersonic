package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.core.query.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.core.knowledge.DictWord;
import com.tencent.supersonic.headless.core.knowledge.builder.WordBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class WordService {

    private List<DictWord> preDictWords = new ArrayList<>();

    public List<DictWord> getAllDictWords() {
        SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();
        SemanticSchema semanticSchema = new SemanticSchema(semanticInterpreter.getDataSetSchema());

        List<DictWord> words = new ArrayList<>();

        addWordsByType(DictWordType.DIMENSION, semanticSchema.getDimensions(), words);
        addWordsByType(DictWordType.METRIC, semanticSchema.getMetrics(), words);
        addWordsByType(DictWordType.ENTITY, semanticSchema.getEntities(), words);
        addWordsByType(DictWordType.VALUE, semanticSchema.getDimensionValues(), words);

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
        return metas.stream()
                .collect(Collectors.toMap(SchemaElement::getId, Function.identity(), (e1, e2) -> e1))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

}
