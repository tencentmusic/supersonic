package com.tencent.supersonic.chat.server.plugin.build;

import lombok.Data;

@Data
public class ParamOption {

    private ParamType paramType;

    private OptionType optionType;

    private String key;

    private String name;

    private String keyAlias;

    private Long modelId;

    private Long elementId;

    private Object value;

    /**
     * CUSTOM: the value is specified by the user
     * SEMANTIC: the value of element
     * FORWARD: only forward
     */
    public enum ParamType {
        CUSTOM, SEMANTIC, FORWARD
    }

    public enum OptionType {
        REQUIRED, OPTIONAL
    }

}
