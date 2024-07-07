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
public class EntityWordBuilder extends BaseWordWithAliasBuilder {

    @Override
    public List<DictWord> doGet(String word, SchemaElement schemaElement) {
        List<DictWord> result = Lists.newArrayList();
        if (Objects.isNull(schemaElement)) {
            return result;
        }
        result.add(getOneWordNature(word, schemaElement, false));
        result.addAll(getOneWordNatureAlias(schemaElement, false));
        return result;

    }

    @Override
    public DictWord getOneWordNature(String word, SchemaElement schemaElement, boolean isSuffix) {
        String nature = DictWordType.NATURE_SPILT + schemaElement.getModel()
                + DictWordType.NATURE_SPILT + schemaElement.getId() + DictWordType.ENTITY.getType();
        DictWord dictWord = new DictWord();
        dictWord.setWord(word);
        dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY * 2, nature));
        return dictWord;
    }

}
