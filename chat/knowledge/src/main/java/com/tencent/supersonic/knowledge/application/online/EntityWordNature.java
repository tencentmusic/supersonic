package com.tencent.supersonic.knowledge.application.online;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * dimension value wordNature
 */
@Service
@Slf4j
public class EntityWordNature extends BaseWordNature {

    @Override
    public List<WordNature> getWordNature(String word, ItemDO itemDO) {
        List<WordNature> result = Lists.newArrayList();
        WordNature wordNature = new WordNature();
        wordNature.setWord(word);
        Integer domain = itemDO.getDomain();
        String nature = NatureType.NATURE_SPILT + domain + NatureType.NATURE_SPILT + itemDO.getItemId()
                + NatureType.ENTITY.getType();
        wordNature.setNatureWithFrequency(String.format("%s 200000", nature));
        result.add(wordNature);
        return result;
    }

}
