package com.tencent.supersonic.headless.core.chat.knowledge.builder;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.core.chat.knowledge.DictWord;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * model word nature
 */
@Service
@Slf4j
public class ModelWordBuilder extends BaseWordWithAliasBuilder {

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

    public DictWord getOneWordNature(String word, SchemaElement schemaElement, boolean isSuffix) {
        DictWord dictWord = new DictWord();
        dictWord.setWord(word);
        String nature = DictWordType.NATURE_SPILT + schemaElement.getDataSet();
        dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY, nature));
        return dictWord;
    }

}
