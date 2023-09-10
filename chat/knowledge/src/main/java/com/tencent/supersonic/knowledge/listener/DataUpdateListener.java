package com.tencent.supersonic.knowledge.listener;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DataUpdateEvent;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataUpdateListener implements ApplicationListener<DataUpdateEvent> {
    @Override
    public void onApplicationEvent(DataUpdateEvent event) {
        DictWord dictWord = new DictWord();
        dictWord.setWord(event.getName());
        String sign = DictWordType.NATURE_SPILT;
        String nature = sign + event.getModelId() + sign + event.getId() + event.getType();
        String natureWithFrequency = nature + " " + Constants.DEFAULT_FREQUENCY;
        dictWord.setNature(nature);
        dictWord.setNatureWithFrequency(natureWithFrequency);
        log.info("dataUpdateListener begins to update data:{}", dictWord);
        HanlpHelper.removeFromCustomDictionary(dictWord);
        dictWord.setWord(event.getNewName());
        HanlpHelper.addToCustomDictionary(dictWord);
    }
}
