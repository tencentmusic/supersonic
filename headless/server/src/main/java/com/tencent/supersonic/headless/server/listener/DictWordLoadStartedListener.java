package com.tencent.supersonic.headless.server.listener;

import com.tencent.supersonic.headless.server.service.impl.DictWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
public class DictWordLoadStartedListener implements CommandLineRunner {

    @Autowired
    private DictWordService dictWordService;

    @Override
    public void run(String... args) {
        updateKnowledgeDimValue();
    }

    public void updateKnowledgeDimValue() {
        try {
            log.debug("ApplicationStartedInit start");
            dictWordService.loadDictWord();
            log.debug("ApplicationStartedInit end");
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
            dictWordService.reloadDictWord();
        } catch (Exception e) {
            log.error("reloadKnowledge error", e);
        }
        log.debug("reloadKnowledge end");
    }
}
