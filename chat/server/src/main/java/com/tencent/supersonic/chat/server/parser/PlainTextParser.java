package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import org.apache.commons.lang3.StringUtils;

public class PlainTextParser implements ChatQueryParser {

    public static final String APP_KEY = "SMALL_TALK";

    public boolean accept(ParseContext parseContext) {
        return !parseContext.getAgent().containsAnyTool();
    }

    @Override
    public void parse(ParseContext parseContext) {
        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode("PLAIN_TEXT");
        parseInfo.setId(1);

        Agent chatAgent = parseContext.getAgent();
        ChatApp chatApp = chatAgent.getChatAppConfig().get(APP_KEY);
        ChatModelConfig modelConfig = chatApp.getChatModelConfig();
        if (modelConfig != null && StringUtils.isNotBlank(modelConfig.getProvider())
                && "DIFY".equalsIgnoreCase(modelConfig.getProvider())) {
            parseInfo.setStream(true);
        }

        parseContext.getResponse().getSelectedParses().add(parseInfo);
        parseContext.getResponse().setState(ParseResp.ParseState.COMPLETED);
    }
}
