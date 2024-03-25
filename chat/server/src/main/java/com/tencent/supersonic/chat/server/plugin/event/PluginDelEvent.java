package com.tencent.supersonic.chat.server.plugin.event;


import com.tencent.supersonic.chat.server.plugin.Plugin;
import org.springframework.context.ApplicationEvent;

public class PluginDelEvent extends ApplicationEvent {

    private Plugin plugin;

    public PluginDelEvent(Object source, Plugin plugin) {
        super(source);
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
