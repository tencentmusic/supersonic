package com.tencent.supersonic.chat.application.parser.resolver;

import com.tencent.supersonic.common.enums.AggregateTypeEnum;

/***
 * aggregate parser
 */
public interface AggregateTypeResolver {

    AggregateTypeEnum resolve(String queryText);

    boolean hasCompareIntentionalWords(String queryText);

}
