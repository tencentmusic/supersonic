package com.tencent.supersonic.chat.server.agent;

import com.tencent.supersonic.chat.api.pojo.enums.DefaultShowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisualConfig {

    private DefaultShowType defaultShowType;

}
