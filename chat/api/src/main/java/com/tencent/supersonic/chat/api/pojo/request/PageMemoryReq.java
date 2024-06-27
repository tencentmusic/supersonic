package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.PageBaseReq;
import lombok.Data;


@Data
public class PageMemoryReq extends PageBaseReq {

    private ChatMemoryFilter chatMemoryFilter = new ChatMemoryFilter();

}
