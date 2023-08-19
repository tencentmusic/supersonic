package com.tencent.supersonic.chat.parser.llm.dsl;

import com.tencent.supersonic.common.util.DateUtils;

public class DSLDateHelper {

    public static String getCurrentDate(Long modelId) {
        return DateUtils.getBeforeDate(4);
//        ChatConfigFilter filter = new ChatConfigFilter();
//        filter.setModelId(modelId);
//
//        List<ChatConfigResp> configResps = ContextUtils.getBean(ConfigService.class).search(filter, null);
//        if (CollectionUtils.isEmpty(configResps)) {
//            return
//        }
//        ChatConfigResp chatConfigResp = configResps.get(0);
//        chatConfigResp.getChatDetailConfig().getChatDefaultConfig().get

    }
}
