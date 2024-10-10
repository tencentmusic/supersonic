package com.tencent.supersonic.common.pojo.enums;

import lombok.Getter;

@Getter
public enum ChatModelType {
    TEXT_TO_SQL("SQL生成", "Convert text query to SQL statement"),
    MULTI_TURN_REWRITE("多轮改写", "Rewrite text query for multi-turn conversation"),
    MEMORY_REVIEW("记忆评估", "Review memory in order to add few-shot examples"),
    RESPONSE_GENERATE("回复生成", "Generate readable response to the end user");

    private String description;
    private String name;

    ChatModelType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
