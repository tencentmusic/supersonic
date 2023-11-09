package com.tencent.supersonic.chat.query.llm;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.query.BaseSemanticQuery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LLMSemanticQuery extends BaseSemanticQuery {

    @Override
    public void initS2Sql(User user) {

    }
}
