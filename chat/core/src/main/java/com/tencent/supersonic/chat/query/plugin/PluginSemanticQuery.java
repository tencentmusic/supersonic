package com.tencent.supersonic.chat.query.plugin;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PluginSemanticQuery implements SemanticQuery {

    protected SemanticParseInfo parseInfo = new SemanticParseInfo();

    public void setParseInfo(SemanticParseInfo parseInfo) {
        this.parseInfo = parseInfo;
    }

    public SemanticParseInfo getParseInfo() {
        return parseInfo;
    }

    @Override
    public ExplainResp explain(User user) {
        return null;
    }
}
