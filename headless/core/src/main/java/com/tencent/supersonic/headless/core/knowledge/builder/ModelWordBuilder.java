package com.tencent.supersonic.headless.core.knowledge.builder;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.core.knowledge.DictWord;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * model word nature
 */
@Service
@Slf4j
public class ModelWordBuilder extends BaseWordBuilder {

    @Override
    public List<DictWord> doGet(String word, SchemaElement schemaElement) {
        List<DictWord> result = Lists.newArrayList();
        //modelName
        DictWord dictWord = buildDictWord(word, schemaElement.getDataSet());
        result.add(dictWord);
        //alias
        List<String> aliasList = schemaElement.getAlias();
        if (CollectionUtils.isNotEmpty(aliasList)) {
            for (String alias : aliasList) {
                result.add(buildDictWord(alias, schemaElement.getDataSet()));
            }
        }
        return result;
    }

    private DictWord buildDictWord(String word, Long modelId) {
        DictWord dictWord = new DictWord();
        dictWord.setWord(word);
        String nature = DictWordType.NATURE_SPILT + modelId;
        dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY, nature));
        return dictWord;
    }

}
