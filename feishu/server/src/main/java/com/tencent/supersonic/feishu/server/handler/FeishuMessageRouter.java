package com.tencent.supersonic.feishu.server.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeishuMessageRouter {

    private final QueryMessageHandler queryHandler;
    private final ExportMessageHandler exportHandler;
    private final HelpMessageHandler helpHandler;
    private final TemplateListHandler templateListHandler;
    private final HistoryMessageHandler historyHandler;
    private final PreviewMessageHandler previewHandler;
    private final UseAgentHandler useAgentHandler;

    public MessageHandler route(String text) {
        if (text == null || text.isBlank())
            return helpHandler;
        String trimmed = text.trim().toLowerCase();
        // Prefix-match commands (have arguments after the command)
        if (trimmed.startsWith("/sql ") || trimmed.equals("/sql")) {
            return previewHandler;
        }
        if (trimmed.startsWith("/use ") || trimmed.equals("/use")) {
            return useAgentHandler;
        }
        return switch (trimmed) {
            case "/help", "帮助" -> helpHandler;
            case "/export", "导出" -> exportHandler;
            case "/history", "历史" -> historyHandler;
            case "/template", "模板" -> templateListHandler;
            default -> queryHandler;
        };
    }
}
