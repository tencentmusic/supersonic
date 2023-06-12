package com.tencent.supersonic.knowledge.application.online;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Metric WordNature
 */
@Service
public class MetricWordNature extends BaseWordNature {

    @Value("${nlp.metric.use.suffix:true}")
    private boolean nlpMetricUseSuffix = true;

    @Override
    public List<WordNature> getWordNature(String word, ItemDO itemDO) {
        List<WordNature> result = Lists.newArrayList();
        result.add(getOnwWordNature(word, itemDO, false));
        if (nlpMetricUseSuffix) {
            String reverseWord = StringUtils.reverse(word);
            if (!word.equalsIgnoreCase(reverseWord)) {
                result.add(getOnwWordNature(reverseWord, itemDO, true));
            }
        }
        return result;
    }

    private WordNature getOnwWordNature(String word, ItemDO itemDO, boolean isSuffix) {
        WordNature wordNature = new WordNature();
        wordNature.setWord(word);
        Integer classId = itemDO.getDomain();
        String nature = NatureType.NATURE_SPILT + classId + NatureType.NATURE_SPILT + itemDO.getItemId()
                + NatureType.METRIC.getType();
        if (isSuffix) {
            nature = NatureType.NATURE_SPILT + classId + NatureType.NATURE_SPILT + itemDO.getItemId()
                    + NatureType.SUFFIX.getType() + NatureType.METRIC.getType();
        }
        wordNature.setNatureWithFrequency(String.format("%s 100000", nature));
        return wordNature;
    }

}
