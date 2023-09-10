package com.tencent.supersonic.chat.utils;

import com.tencent.supersonic.chat.config.ChatConfig;
import org.springframework.context.ApplicationEvent;

public class VisibilityEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;
    private ChatConfig chatConfig;

    public VisibilityEvent(Object source, ChatConfig chatConfig) {
        super(source);
        this.chatConfig = chatConfig;
    }

    public void setChatConfig(ChatConfig chatConfig) {
        this.chatConfig = chatConfig;
    }

    public ChatConfig getChatConfig() {
        return chatConfig;
    }
}
