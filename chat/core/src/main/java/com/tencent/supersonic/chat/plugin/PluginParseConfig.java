package com.tencent.supersonic.chat.plugin;


import com.tencent.supersonic.chat.parser.plugin.function.Parameters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class PluginParseConfig implements Serializable {

    public Parameters parameters;

    public List<String> examples;

    private String name;

    private String description;

}
