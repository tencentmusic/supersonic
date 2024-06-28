package com.tencent.supersonic.chat.server.plugin.event;

import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import org.springframework.context.ApplicationEvent;

public class PluginUpdateEvent extends ApplicationEvent {

    private ChatPlugin oldPlugin;

    private ChatPlugin newPlugin;

    public PluginUpdateEvent(Object source, ChatPlugin oldPlugin, ChatPlugin newPlugin) {
        super(source);
        this.oldPlugin = oldPlugin;
        this.newPlugin = newPlugin;
    }

    public ChatPlugin getOldPlugin() {
        return oldPlugin;
    }

    public ChatPlugin getNewPlugin() {
        return newPlugin;
    }

}
