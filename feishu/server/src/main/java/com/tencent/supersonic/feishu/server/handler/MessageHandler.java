package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;

public interface MessageHandler {
    void handle(FeishuMessage msg, User user);
}
