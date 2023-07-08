package com.tencent.supersonic.chat.domain.pojo.chat;

import java.util.List;
import lombok.Data;

@Data
public class LLMSchema {

    private String domainName;

    private List<String> fieldNameList;

}
