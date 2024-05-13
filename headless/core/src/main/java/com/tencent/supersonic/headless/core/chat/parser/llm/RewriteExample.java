package com.tencent.supersonic.headless.core.chat.parser.llm;

import lombok.Data;

@Data
public class RewriteExample {

    private String contextualQuestions;

    private String currentQuestion;

    private String rewritingCurrentQuestion;

}
