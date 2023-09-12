package com.tencent.supersonic.knowledge.dictionary.builder;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.knowledge.dictionary.DictWordType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * dimension value wordNature
 */
@Service
@Slf4j
public class ValueWordBuilder extends BaseWordBuilder {

    @Override
    public List<DictWord> doGet(String word, SchemaElement schemaElement) {

        List<DictWord> result = Lists.newArrayList();
        if (Objects.nonNull(schemaElement) && !CollectionUtils.isEmpty(schemaElement.getAlias())) {

            schemaElement.getAlias().stream().forEach(value -> {
                DictWord dictWord = new DictWord();
                Long modelId = schemaElement.getModel();
                String nature = DictWordType.NATURE_SPILT + modelId + DictWordType.NATURE_SPILT + schemaElement.getId();
                dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY, nature));
                dictWord.setWord(value);
                result.add(dictWord);
            });
        }
        log.debug("ValueWordBuilder, result:{}", result);
        return result;
    }

}
