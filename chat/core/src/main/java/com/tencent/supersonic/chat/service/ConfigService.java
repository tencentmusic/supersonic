package com.tencent.supersonic.chat.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;

import java.util.List;

public interface ConfigService {

    Long addConfig(ChatConfigBaseReq extendBaseCmd, User user);

    Long editConfig(ChatConfigEditReqReq extendEditCmd, User user);

    List<ChatConfigResp> search(ChatConfigFilter filter, User user);

    ChatConfigRichResp getConfigRichInfo(Long modelId);

    ChatConfigResp fetchConfigByModelId(Long modelId);

    List<ChatConfigRichResp> getAllChatRichConfig();
}
