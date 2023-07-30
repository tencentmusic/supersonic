package com.tencent.supersonic.knowledge.dictionary.builder;

import com.google.common.collect.Lists;

import java.util.List;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.knowledge.dictionary.DictWordType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * dimension value wordNature
 */
@Service
@Slf4j
public class EntityWordBuilder extends BaseWordBuilder {

    @Override
    public List<DictWord> doGet(String word, SchemaElement schemaElement) {
        List<DictWord> result = Lists.newArrayList();
        DictWord dictWord = new DictWord();
        dictWord.setWord(word);
        Long domain = schemaElement.getDomain();
        String nature = DictWordType.NATURE_SPILT + domain + DictWordType.NATURE_SPILT + schemaElement.getId()
                + DictWordType.ENTITY.getType();
        dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY * 2, nature));
        result.add(dictWord);
        return result;
    }

}
