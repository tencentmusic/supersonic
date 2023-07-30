package com.tencent.supersonic.chat.config;

import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
public class ChatConfigFilter {

    private Long id;
    private Long domainId;
    private StatusEnum status = StatusEnum.ONLINE;
}