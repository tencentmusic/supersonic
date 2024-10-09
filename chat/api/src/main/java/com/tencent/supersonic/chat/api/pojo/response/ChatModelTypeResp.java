package com.tencent.supersonic.chat.api.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatModelTypeResp {
    private String type;
    private String name;
    private String description;
}
