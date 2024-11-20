package com.tencent.supersonic.common.pojo.enums;

public enum Text2SQLType {
    ONLY_RULE, LLM_OR_RULE, NONE;

    public boolean enableLLM() {
        return this.equals(LLM_OR_RULE);
    }
}
