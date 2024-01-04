package com.tencent.supersonic.chat.server.service;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.request.ItemNameVisibilityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.server.config.ChatConfig;

import java.util.List;
import java.util.Map;

public interface ConfigService {

    Long addConfig(ChatConfigBaseReq extendBaseCmd, User user);

    Long editConfig(ChatConfigEditReqReq extendEditCmd, User user);

    ItemNameVisibilityInfo getItemNameVisibility(ChatConfig chatConfig);

    ItemNameVisibilityInfo getVisibilityByModelId(Long modelId);

    List<ChatConfigResp> search(ChatConfigFilter filter, User user);

    ChatConfigRichResp getConfigRichInfo(Long modelId);

    ChatConfigResp fetchConfigByModelId(Long modelId);

    List<ChatConfigRichResp> getAllChatRichConfig();

    Map<Long, ChatConfigRichResp> getModelIdToChatRichConfig();
}
