package com.tencent.supersonic.common.pojo.enums;

public enum Text2SQLType {

    ONLY_RULE, ONLY_LLM, RULE_AND_LLM;

    public boolean enableRule() {
        return this.equals(ONLY_RULE) || this.equals(RULE_AND_LLM);
    }

    public boolean enableLLM() {
        return this.equals(ONLY_LLM) || this.equals(RULE_AND_LLM);
    }

}
