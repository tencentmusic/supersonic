package com.tencent.supersonic.common.pojo.enums;

public enum Text2SQLType {
    ONLY_RULE, ONLY_LLM, LLM_OR_RULE;

    public boolean enableRule() {
        return this.equals(ONLY_RULE) || this.equals(LLM_OR_RULE);
    }

    public boolean enableLLM() {
        return this.equals(ONLY_LLM) || this.equals(LLM_OR_RULE);
    }
}
