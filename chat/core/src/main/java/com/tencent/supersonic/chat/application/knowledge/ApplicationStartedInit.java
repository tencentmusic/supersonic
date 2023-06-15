package com.tencent.supersonic.chat.application.knowledge;

import com.tencent.supersonic.common.nlp.WordNature;
import com.tencent.supersonic.knowledge.domain.service.OnlineKnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ApplicationStartedInit implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private OnlineKnowledgeService onlineKnowledgeService;

    @Autowired
    private WordNatureService wordNatureService;


    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            log.info("ApplicationStartedInit start");

            List<WordNature> wordNatures = wordNatureService.getAllWordNature();

            wordNatureService.setPreWordNatures(wordNatures);

            onlineKnowledgeService.reloadAllData(wordNatures);

            log.info("ApplicationStartedInit end");
        } catch (Exception e) {
            log.error("ApplicationStartedInit error", e);
        }
    }

    /***
     * reload knowledge task
     */
    @Scheduled(cron = "${reload.knowledge.corn:0 0/1 * * * ?}")
    public void reloadKnowledge() {
        log.debug("reloadKnowledge start");

        try {
            List<WordNature> wordNatures = wordNatureService.getAllWordNature();
            List<WordNature> preWordNatures = wordNatureService.getPreWordNatures();

            if (CollectionUtils.isEqualCollection(wordNatures, preWordNatures)) {
                log.debug("wordNatures is not change, reloadKnowledge end");
                return;
            }
            wordNatureService.setPreWordNatures(wordNatures);
            onlineKnowledgeService.updateOnlineKnowledge(wordNatureService.getAllWordNature());
            wordNatureService.getCache().refresh("");

        } catch (Exception e) {
            log.error("reloadKnowledge error", e);
        }

        log.debug("reloadKnowledge end");
    }
}