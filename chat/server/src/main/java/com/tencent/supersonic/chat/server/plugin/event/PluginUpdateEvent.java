package com.tencent.supersonic.chat.server.plugin.event;

import com.tencent.supersonic.chat.server.plugin.Plugin;
import org.springframework.context.ApplicationEvent;

public class PluginUpdateEvent extends ApplicationEvent {

    private Plugin oldPlugin;

    private Plugin newPlugin;

    public PluginUpdateEvent(Object source, Plugin oldPlugin, Plugin newPlugin) {
        super(source);
        this.oldPlugin = oldPlugin;
        this.newPlugin = newPlugin;
    }

    public Plugin getOldPlugin() {
        return oldPlugin;
    }

    public Plugin getNewPlugin() {
        return newPlugin;
    }

}
