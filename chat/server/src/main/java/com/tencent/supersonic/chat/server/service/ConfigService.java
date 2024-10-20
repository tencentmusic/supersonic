package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.request.ItemNameVisibilityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.server.config.ChatConfig;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;

public interface ConfigService {

    Long addConfig(ChatConfigBaseReq extendBaseCmd, User user);

    Long editConfig(ChatConfigEditReqReq extendEditCmd, User user);

    ItemNameVisibilityInfo getItemNameVisibility(ChatConfig chatConfig);

    List<ChatConfigResp> search(ChatConfigFilter filter, User user);

    ChatConfigRichResp getConfigRichInfo(Long modelId);

    ChatConfigResp fetchConfigByModelId(Long modelId);

    List<ChatConfigRichResp> getAllChatRichConfig();
}
