package com.tencent.supersonic.chat.domain.repository;


import com.tencent.supersonic.chat.domain.pojo.config.ChatConfig;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilter;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import java.util.List;


public interface ChatConfigRepository {

    Long createConfig(ChatConfig chaConfig);

    Long updateConfig(ChatConfig chaConfig);

    List<ChatConfigInfo> getChatConfig(ChatConfigFilter filter);

    ChatConfigInfo getConfigByDomainId(Long domainId);
}
