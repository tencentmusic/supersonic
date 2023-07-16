package com.tencent.supersonic.chat.domain.pojo.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * advanced knowledge config
 */
@Data
public class KnowledgeAdvancedConfig {

    private List<String> blackList = new ArrayList<>();
    private List<String> whiteList = new ArrayList<>();
    private List<String> ruleList = new ArrayList<>();
}