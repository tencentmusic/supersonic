package com.tencent.supersonic.chat.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.config.*;

import java.util.List;

public interface ConfigService {

    Long addConfig(ChatConfigBaseReq extendBaseCmd, User user);

    Long editConfig(ChatConfigEditReqReq extendEditCmd, User user);

    List<ChatConfigResp> search(ChatConfigFilter filter, User user);

    ChatConfigRich getConfigRichInfo(Long domainId);

    ChatConfigResp fetchConfigByDomainId(Long domainId);

    List<ChatConfigRich> getAllChatRichConfig();
}
