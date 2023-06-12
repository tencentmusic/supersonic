package com.tencent.supersonic.chat.domain.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigBase;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigEditReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilter;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import java.util.List;

public interface ConfigService {

    Long addConfig(ChatConfigBase extendBaseCmd, User user);

    Long editConfig(ChatConfigEditReq extendEditCmd, User user);

    List<ChatConfigInfo> search(ChatConfigFilter filter, User user);

    ChatConfigRichInfo getConfigRichInfo(Long domainId);
}
