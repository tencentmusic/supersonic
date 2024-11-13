package com.tencent.supersonic.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeneralManageConfig {

    // 使用 @Value 注解注入配置文件中的 chat.model.ids 配置
    @Value("${chat.model.ids:1,2}")
    private List<Integer> chatModelIds;

    @Value("${chat.database.ids:1,2}")
    private List<Long> chatDatabaseIds;

    @Value("${chat.domain.ids:1,2}")
    private List<Long> chatDomainIds;

    // 获取通用模型的 IDs
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
