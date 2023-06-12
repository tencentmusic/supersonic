package com.tencent.supersonic.knowledge.application.online;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * domain word nature
 */
@Service
@Slf4j
public class DomainWordNature extends BaseWordNature {

    @Override
    public List<WordNature> getWordNature(String word, ItemDO itemDO) {
        List<WordNature> result = Lists.newArrayList();
        WordNature wordNature = new WordNature();
        wordNature.setWord(word);
        Integer classId = itemDO.getDomain();
        String nature = NatureType.NATURE_SPILT + classId;
        wordNature.setNatureWithFrequency(String.format("%s 100000", nature));
        result.add(wordNature);
        return result;
    }
}
