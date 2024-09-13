package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ChatConfigFilter {

    private Long id;
    private Long modelId;
    private StatusEnum status = StatusEnum.ONLINE;
}
