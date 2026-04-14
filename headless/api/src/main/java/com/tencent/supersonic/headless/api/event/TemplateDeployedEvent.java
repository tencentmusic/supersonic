package com.tencent.supersonic.headless.api.event;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig;
import org.springframework.context.ApplicationEvent;

public class TemplateDeployedEvent extends ApplicationEvent {

    private final SemanticDeployResult result;
    private final SemanticTemplateConfig config;
    private final User user;

    public TemplateDeployedEvent(Object source, SemanticDeployResult result,
            SemanticTemplateConfig config, User user) {
        super(source);
        this.result = result;
        this.config = config;
        this.user = user;
    }

    public SemanticDeployResult getResult() {
        return result;
    }

    public SemanticTemplateConfig getConfig() {
        return config;
    }

    public User getUser() {
        return user;
    }
}
