package com.tencent.supersonic.chat.persistence.repository;


import com.tencent.supersonic.chat.config.ChatConfig;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;

import java.util.List;

public interface ChatConfigRepository {

    Long createConfig(ChatConfig chaConfig);

    Long updateConfig(ChatConfig chaConfig);

    List<ChatConfigResp> getChatConfig(ChatConfigFilter filter);

    ChatConfigResp getConfigByModelId(Long modelId);
}
