package com.tencent.supersonic.headless.chat.knowledge.builder;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ValueWordBuilder extends BaseWordWithAliasBuilder {

    @Override
    public List<DictWord> doGet(String word, SchemaElement schemaElement) {
        List<DictWord> result = Lists.newArrayList();
        if (Objects.nonNull(schemaElement)) {
            result.addAll(getOneWordNatureAlias(schemaElement, false));
        }
        return result;
    }

    public DictWord getOneWordNature(String word, SchemaElement schemaElement, boolean isSuffix) {
        DictWord dictWord = new DictWord();
        Long modelId = schemaElement.getModel();
        String nature = DictWordType.NATURE_SPILT + modelId + DictWordType.NATURE_SPILT + schemaElement.getId();
        dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY, nature));
        dictWord.setWord(word);
        return dictWord;
    }

}
