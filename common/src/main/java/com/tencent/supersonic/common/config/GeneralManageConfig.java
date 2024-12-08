package com.tencent.supersonic.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeneralManageConfig {

    @Value("${s2.manage.chat.agent.ids:1,3,5}")
    private List<Long> chatAgentIds;

    @Value("${s2.manage.chat.model.ids:9}")
    private List<Integer> chatModelIds;

    @Value("${s2.manage.chat.dataBase.ids:1}")
    private List<Long> chatDatabaseIds;

    @Value("${s2.manage.chat.domain.ids:1,3}")
    private List<Long> chatDomainIds;

    public List<Long> getChatAgentIds() {
        return chatAgentIds;
    }
    public List<Integer> getChatModelIds() {
        return chatModelIds;
    }

    public List<Long> getChatDatabaseIds() {
        return chatDatabaseIds;
    }

    public List<Long> getChatDomainIds() {
        return chatDomainIds;
    }
}
