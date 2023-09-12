package com.tencent.supersonic.chat.parser.plugin.function;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class Parameters {

    //default: object
    private String type = "object";

    private Map<String, FunctionFiled> properties;

    private List<String> required;

}
