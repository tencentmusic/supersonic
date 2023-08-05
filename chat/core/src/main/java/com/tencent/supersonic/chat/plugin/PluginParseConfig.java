package com.tencent.supersonic.chat.plugin;


import com.tencent.supersonic.chat.parser.function.Parameters;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PluginParseConfig implements Serializable {

    private String name;

    private String description;

    public Parameters parameters;

    public List<String> examples;

}
