package com.tencent.supersonic.knowledge.application.online;

import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * base word nature
 */
@Slf4j
public abstract class BaseWordNature {

    /**
     * 获取所有wordNature
     *
     * @param itemDOS
     * @return
     */
    public List<WordNature> getWordNatureList(List<ItemDO> itemDOS) {
        List<WordNature> wordNatures = new ArrayList<>();
        try {
            wordNatures = getWordNaturesWithException(itemDOS);
        } catch (Exception e) {
            log.error("getWordNatureList error,", e);
        }
        return wordNatures;
    }

    public List<WordNature> getWordNaturesWithException(List<ItemDO> itemDOS) {

        List<WordNature> wordNatures = new ArrayList<>();

        for (ItemDO itemDO : itemDOS) {
            wordNatures.addAll(getWordNature(itemDO.getName(), itemDO));
        }
        return wordNatures;
    }

    public abstract List<WordNature> getWordNature(String word, ItemDO itemDO);

    public Integer getElementID(String nature) {
        String[] split = nature.split(NatureType.NATURE_SPILT);
        if (split.length >= 3) {
            return Integer.valueOf(split[2]);
        }
        return 0;
    }

    public static Integer getDomain(String nature) {
        String[] split = nature.split(NatureType.NATURE_SPILT);
        return Integer.valueOf(split[1]);
    }

}
