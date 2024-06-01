package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class NL2PluginParser implements ChatParser {

    private final List<PluginRecognizer> pluginRecognizers = ComponentFactory.getPluginRecognizers();

    @Override
    public void parse(ChatParseContext chatParseContext, ParseResp parseResp) {
        pluginRecognizers.forEach(pluginRecognizer -> {
            pluginRecognizer.recognize(chatParseContext, parseResp);
            log.info("{} recallResult:{}", pluginRecognizer.getClass().getSimpleName(),
                    JsonUtil.toString(parseResp));
        });
    }

}
