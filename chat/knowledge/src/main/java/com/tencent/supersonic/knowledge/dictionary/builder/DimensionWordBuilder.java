package com.tencent.supersonic.knowledge.dictionary.builder;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * dimension word nature
 */
@Service
public class DimensionWordBuilder extends BaseWordBuilder {

    @Value("${nlp.dimension.use.suffix:true}")
    private boolean nlpDimensionUseSuffix = true;


    @Override
    public List<DictWord> doGet(String word, SchemaElement schemaElement) {
        List<DictWord> result = Lists.newArrayList();
        result.add(getOnwWordNature(word, schemaElement, false));
        result.addAll(getOnwWordNatureAlias(schemaElement, false));
        if (nlpDimensionUseSuffix) {
            String reverseWord = StringUtils.reverse(word);
            if (StringUtils.isNotEmpty(word) && !word.equalsIgnoreCase(reverseWord)) {
                result.add(getOnwWordNature(reverseWord, schemaElement, true));
            }
        }
        return result;
    }

    private DictWord getOnwWordNature(String word, SchemaElement schemaElement, boolean isSuffix) {
        DictWord dictWord = new DictWord();
        dictWord.setWord(word);
        Long domainId = schemaElement.getModel();
        String nature = DictWordType.NATURE_SPILT + domainId + DictWordType.NATURE_SPILT + schemaElement.getId()
                + DictWordType.DIMENSION.getType();
        if (isSuffix) {
            nature = DictWordType.NATURE_SPILT + domainId + DictWordType.NATURE_SPILT + schemaElement.getId()
                    + DictWordType.SUFFIX.getType() + DictWordType.DIMENSION.getType();
        }
        dictWord.setNatureWithFrequency(String.format("%s " + DEFAULT_FREQUENCY, nature));
        return dictWord;
    }

    private List<DictWord> getOnwWordNatureAlias(SchemaElement schemaElement, boolean isSuffix) {
        List<DictWord> dictWords = new ArrayList<>();
        if (CollectionUtils.isEmpty(schemaElement.getAlias())) {
            return dictWords;
        }

        for (String alias : schemaElement.getAlias()) {
            dictWords.add(getOnwWordNature(alias, schemaElement, false));
        }
        return dictWords;
    }

}
