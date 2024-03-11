package com.tencent.supersonic.chat.server.plugin;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class PluginParseConfig implements Serializable {

    public List<String> examples;

    private String name;

    private String description;

}
