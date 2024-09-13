package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.headless.chat.ChatQueryContext;

import java.util.Set;

public interface DataSetResolver {

    Long resolve(ChatQueryContext chatQueryContext, Set<Long> restrictiveModels);
}
