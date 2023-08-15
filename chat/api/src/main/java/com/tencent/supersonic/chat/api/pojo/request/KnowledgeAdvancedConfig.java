package com.tencent.supersonic.chat.api.pojo.request;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * advanced knowledge config
 */
@Data
public class KnowledgeAdvancedConfig {

    private List<String> blackList = new ArrayList<>();
    private List<String> whiteList = new ArrayList<>();
    private List<String> ruleList = new ArrayList<>();
}