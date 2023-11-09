package com.tencent.supersonic.chat.query.plugin;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.query.BaseSemanticQuery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PluginSemanticQuery extends BaseSemanticQuery {

    @Override
    public String explain(User user) {
        return null;
    }

    @Override
    public void initS2Sql(User user) {

    }
}
