package com.tencent.supersonic.knowledge.listener;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DataDeleteEvent;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataDeleteListener implements ApplicationListener<DataDeleteEvent> {
    @Override
    public void onApplicationEvent(DataDeleteEvent event) {
        DictWord dictWord = new DictWord();
        dictWord.setWord(event.getName());
        String sign = DictWordType.NATURE_SPILT;
        String nature = sign + event.getModelId() + sign + event.getId() + event.getType();
        String natureWithFrequency = nature + " " + Constants.DEFAULT_FREQUENCY;
        dictWord.setNature(nature);
        dictWord.setNatureWithFrequency(natureWithFrequency);
        log.info("dataDeleteListener begins to delete data:{}", dictWord);
        HanlpHelper.removeFromCustomDictionary(dictWord);
    }
}
