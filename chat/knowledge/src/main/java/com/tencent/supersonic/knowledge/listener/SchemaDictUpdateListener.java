package com.tencent.supersonic.knowledge.listener;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class SchemaDictUpdateListener implements ApplicationListener<DataEvent> {

    @Autowired
    private SchemaService schemaService;

    @Async
    @Override
    public void onApplicationEvent(DataEvent dataEvent) {
        if (CollectionUtils.isEmpty(dataEvent.getDataItems())) {
            return;
        }
        schemaService.getCache().invalidateAll();
        dataEvent.getDataItems().forEach(dataItem -> {
            DictWord dictWord = new DictWord();
            dictWord.setWord(dataItem.getName());
            String sign = DictWordType.NATURE_SPILT;
            String nature = sign + dataItem.getModelId() + sign + dataItem.getId()
                    + sign + dataItem.getType().getName();
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
