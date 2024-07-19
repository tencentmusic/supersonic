package com.tencent.supersonic.chat.server.memory;


import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryFilter;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Slf4j
@Component
@ConditionalOnProperty(name = "s2.task.vector.is-human-review", havingValue = "true")
public class MemoryVectorTask {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AgentService agentService;

    @Scheduled(fixedDelayString = "${s2.task.vector.fixed-delay:60000}")
    public void vector() {
        log.info("Insert the data approved by the administrator into the vector database");
        ChatMemoryFilter chatMemoryFilter = new ChatMemoryFilter();
        chatMemoryFilter.setStatus(MemoryStatus.PENDING);
        chatMemoryFilter.setLlmReviewRet(MemoryReviewResult.POSITIVE);
        chatMemoryFilter.setHumanReviewRet(MemoryReviewResult.POSITIVE);
        memoryService.getMemories(chatMemoryFilter).stream()
                .forEach(m -> {
                    Agent chatAgent = agentService.getAgent(m.getAgentId());
                    if (Objects.nonNull(chatAgent) && chatAgent.enableMemoryReview()) {
                        if (MemoryReviewResult.POSITIVE.equals(m.getLlmReviewRet()) && MemoryReviewResult.POSITIVE.equals(m.getHumanReviewRet())) {
                            memoryService.enableMemory(m);
                            memoryService.updateMemory(m);
                        }

                    } else {
                        log.debug("Agent id {} not found or memory review disabled", m.getAgentId());
                    }
                });

    }
}
