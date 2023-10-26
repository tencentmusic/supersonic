package com.tencent.supersonic.knowledge.dictionary.builder;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * base word nature
 */
@Slf4j
public abstract class BaseWordBuilder {

    public static final Long DEFAULT_FREQUENCY = 100000L;

    public List<DictWord> getDictWords(List<SchemaElement> schemaElements) {
        List<DictWord> dictWords = new ArrayList<>();
        try {
            dictWords = getDictWordsWithException(schemaElements);
        } catch (Exception e) {
            log.error("getWordNatureList error,", e);
        }
        return dictWords;
    }

    protected List<DictWord> getDictWordsWithException(List<SchemaElement> schemaElements) {

        List<DictWord> dictWords = new ArrayList<>();

        for (SchemaElement schemaElement : schemaElements) {
            dictWords.addAll(doGet(schemaElement.getName(), schemaElement));
        }
        return dictWords;
    }

    protected abstract List<DictWord> doGet(String word, SchemaElement schemaElement);
}
