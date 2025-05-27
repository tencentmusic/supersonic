package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NL2PluginParser implements ChatQueryParser {

    private final List<PluginRecognizer> pluginRecognizers =
            ComponentFactory.getPluginRecognizers();

    public boolean accept(ParseContext parseContext) {
        return parseContext.getAgent().containsPluginTool();
    }

    @Override
    public void parse(ParseContext parseContext) {
        pluginRecognizers.forEach(pluginRecognizer -> {
            pluginRecognizer.recognize(parseContext);
            if (parseContext.getResponse().getSelectedParses().size()>=2){
                parseContext.getResponse().setSelectedParses(parseContext.getResponse().getSelectedParses().subList(0, 1));
            }
            log.info("{} recallResult:{}", pluginRecognizer.getClass().getSimpleName(),
                    JsonUtil.toString(parseContext.getResponse()));
        });
    }
}
