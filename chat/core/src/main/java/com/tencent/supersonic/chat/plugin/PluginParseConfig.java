package com.tencent.supersonic.chat.plugin;


import com.tencent.supersonic.chat.parser.function.Parameters;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class PluginParseConfig implements Serializable {

    private String name;

    private String description;

    public Parameters parameters;

    public List<String> examples;

}
