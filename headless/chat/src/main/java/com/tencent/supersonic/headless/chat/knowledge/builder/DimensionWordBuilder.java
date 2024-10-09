package com.tencent.supersonic.headless.chat.knowledge.builder;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/** dimension word nature */
@Service
public class DimensionWordBuilder extends BaseWordWithAliasBuilder {

    @Override
    public List<DictWord> doGet(String word, SchemaElement schemaElement) {
        List<DictWord> result = Lists.newArrayList();
        result.add(getOneWordNature(word, schemaElement, false));
        result.addAll(getOneWordNatureAlias(schemaElement, false));
        String reverseWord = StringUtils.reverse(word);
        if (StringUtils.isNotEmpty(word) && !word.equalsIgnoreCase(reverseWord)) {
            result.add(getOneWordNature(reverseWord, schemaElement, true));
        }
        return result;
    }

    public DictWord getOneWordNature(String word, SchemaElement schemaElement, boolean isSuffix) {
        DictWord dictWord = new DictWord();
        dictWord.setWord(word);
        Long modelId = schemaElement.getModel();
        String nature = DictWordType.NATURE_SPILT + modelId + DictWordType.NATURE_SPILT
                + schemaElement.getId() + DictWordType.DIMENSION.getType();
        if (isSuffix) {
            nature = DictWordType.NATURE_SPILT + modelId + DictWordType.NATURE_SPILT
                    + schemaElement.getId() + DictWordType.SUFFIX.getType()
                    + DictWordType.DIMENSION.getType();
        }
        dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY, nature));
        return dictWord;
    }
}
