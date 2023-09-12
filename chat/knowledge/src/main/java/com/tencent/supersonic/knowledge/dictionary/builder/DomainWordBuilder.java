package com.tencent.supersonic.knowledge.dictionary.builder;

import com.google.common.collect.Lists;

import java.util.List;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.knowledge.dictionary.DictWordType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * domain word nature
 */
@Service
@Slf4j
public class DomainWordBuilder extends BaseWordBuilder {

    @Override
    public List<DictWord> doGet(String word, SchemaElement schemaElement) {
        List<DictWord> result = Lists.newArrayList();
        DictWord dictWord = new DictWord();
        dictWord.setWord(word);
        Long modelId = schemaElement.getModel();
        String nature = DictWordType.NATURE_SPILT + modelId;
        dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY, nature));
        result.add(dictWord);
        return result;
    }

}
