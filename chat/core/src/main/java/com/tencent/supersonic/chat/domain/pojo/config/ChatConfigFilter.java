package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
public class ChatConfigFilter {

    private Long id;
    private Long domainId;
    private StatusEnum status = StatusEnum.ONLINE;
}