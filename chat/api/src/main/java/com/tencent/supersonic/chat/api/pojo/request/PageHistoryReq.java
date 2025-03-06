package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.PageBaseReq;
import lombok.Data;

@Data
public class PageHistoryReq extends PageBaseReq {

    private ChatHistoryFilter chatHistoryFilter = new ChatHistoryFilter();
    private ChatMemoryFilter chatMemoryFilter = new ChatMemoryFilter();
}
