package com.tencent.supersonic.chat.parser.function;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Parameters {

    //default: object
    private String type = "object";

    private Map<String, FunctionFiled> properties;

    private List<String> required;

}
