package com.tencent.supersonic.headless.server.web.service;

import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.QueryContext;

public interface WorkflowService {
    void startWorkflow(QueryContext queryCtx, ChatContext chatCtx, ParseResp parseResult);
}