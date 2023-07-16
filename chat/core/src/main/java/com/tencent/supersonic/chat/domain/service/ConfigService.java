package com.tencent.supersonic.chat.domain.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigBaseReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilter;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichResp;

import java.util.List;

public interface ConfigService {

    Long addConfig(ChatConfigBaseReq extendBaseCmd, User user);

    Long editConfig(ChatConfigEditReqReq extendEditCmd, User user);

    List<ChatConfigResp> search(ChatConfigFilter filter, User user);

    ChatConfigRichResp getConfigRichInfo(Long domainId);

    ChatConfigResp fetchConfigByDomainId(Long domainId);

    List<ChatConfigRichResp> getAllChatRichConfig();
}
