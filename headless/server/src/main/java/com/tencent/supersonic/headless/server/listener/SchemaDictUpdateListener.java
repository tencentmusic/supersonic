package com.tencent.supersonic.headless.server.listener;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class SchemaDictUpdateListener implements ApplicationListener<DataEvent> {

    @Async
    @Override
    public void onApplicationEvent(DataEvent dataEvent) {
        if (CollectionUtils.isEmpty(dataEvent.getDataItems())) {
            return;
        }
        dataEvent.getDataItems().forEach(dataItem -> {
            DictWord dictWord = new DictWord();
            dictWord.setWord(dataItem.getName());
            String sign = DictWordType.NATURE_SPILT;
            String suffixNature = DictWordType.getSuffixNature(dataItem.getType());
            String nature = sign + dataItem.getModelId() + dataItem.getId() + suffixNature;
            String natureWithFrequency = nature + " " + Constants.DEFAULT_FREQUENCY;
            dictWord.setNature(nature);
            dictWord.setNatureWithFrequency(natureWithFrequency);
            if (EventType.ADD.equals(dataEvent.getEventType())) {
                HanlpHelper.addToCustomDictionary(dictWord);
            } else if (EventType.DELETE.equals(dataEvent.getEventType())) {
                HanlpHelper.removeFromCustomDictionary(dictWord);
            } else if (EventType.UPDATE.equals(dataEvent.getEventType())) {
                HanlpHelper.removeFromCustomDictionary(dictWord);
                dictWord.setWord(dataItem.getNewName());
                HanlpHelper.addToCustomDictionary(dictWord);
            }
        });
    }
}
