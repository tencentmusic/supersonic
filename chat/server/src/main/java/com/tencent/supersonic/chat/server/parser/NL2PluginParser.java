package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import java.util.List;

public class NL2PluginParser implements ChatParser {

    private final List<PluginRecognizer> pluginRecognizers = ComponentFactory.getPluginRecognizers();

    @Override
    public void parse(ChatParseContext chatParseContext, ParseResp parseResp) {
        pluginRecognizers.forEach(pluginRecognizer -> {
            pluginRecognizer.recognize(chatParseContext, parseResp);
        });
    }

}
