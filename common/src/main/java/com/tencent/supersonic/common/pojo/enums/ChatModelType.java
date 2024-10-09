package com.tencent.supersonic.common.pojo.enums;

public enum ChatModelType {
    TEXT_TO_SQL("Convert text query to SQL statement"), MULTI_TURN_REWRITE(
            "Rewrite text query for multi-turn conversation"), MEMORY_REVIEW(
                    "Review memory in order to add few-shot examples"), RESPONSE_GENERATE(
                            "Generate readable response to the end user");

    private String description;

    ChatModelType(String description) {
        this.description = description;
    }
}
