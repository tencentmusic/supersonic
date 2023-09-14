package com.tencent.supersonic.knowledge.listener;

import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.knowledge.service.KnowledgeService;
import com.tencent.supersonic.knowledge.service.WordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ApplicationStartedListener implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private KnowledgeService knowledgeService;
    @Autowired
    private WordService wordService;
    @Autowired
    private SchemaService schemaService;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        updateKnowledgeDimValue();
    }

    public Boolean updateKnowledgeDimValue() {
        Boolean isOk = false;
        try {
            log.debug("ApplicationStartedInit start");

            List<DictWord> dictWords = wordService.getAllDictWords();
            wordService.setPreDictWords(dictWords);
            knowledgeService.reloadAllData(dictWords);

            log.debug("ApplicationStartedInit end");
            isOk = true;
        } catch (Exception e) {
            log.error("ApplicationStartedInit error", e);
        }
        return isOk;
    }

    public Boolean updateKnowledgeDimValueAsync() {
        CompletableFuture.supplyAsync(() -> {
            updateKnowledgeDimValue();
            return null;
        });
        return true;
    }

    /***
     * reload knowledge task
     */
    @Scheduled(cron = "${reload.knowledge.corn:0 0/1 * * * ?}")
    public void reloadKnowledge() {
        log.debug("reloadKnowledge start");

        try {
            List<DictWord> dictWords = wordService.getAllDictWords();
            List<DictWord> preDictWords = wordService.getPreDictWords();

            if (CollectionUtils.isEqualCollection(dictWords, preDictWords)) {
                log.debug("dictWords has not changed, reloadKnowledge end");
                return;
            }
            log.info("dictWords has changed");
            wordService.setPreDictWords(dictWords);
            knowledgeService.updateOnlineKnowledge(wordService.getAllDictWords());
            schemaService.getCache().refresh(SchemaService.ALL_CACHE);

        } catch (Exception e) {
            log.error("reloadKnowledge error", e);
        }

        log.debug("reloadKnowledge end");
    }
}
