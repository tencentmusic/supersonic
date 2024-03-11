package com.tencent.supersonic.chat.server.plugin;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

@Data
public class Plugin extends RecordInfo {

    private Long id;

    /***
     * plugin type WEB_PAGE WEB_SERVICE
     */
    private String type;

    private List<Long> dataSetList = Lists.newArrayList();

    /**
     * description, for parsing
     */
    private String pattern;

    /**
     * parse
     */
    private ParseMode parseMode;

    private String parseModeConfig;

    private String name;

    /**
     * config for different plugin type
     */
    private String config;

    private String comment;

    public List<String> getExampleQuestionList() {
        if (StringUtils.isNotBlank(parseModeConfig)) {
            PluginParseConfig pluginParseConfig = JSONObject.parseObject(parseModeConfig, PluginParseConfig.class);
            return pluginParseConfig.getExamples();
        }
        return Lists.newArrayList();
    }

    public boolean isContainsAllModel() {
        return CollectionUtils.isNotEmpty(dataSetList) && dataSetList.contains(-1L);
    }

    public Long getDefaultMode() {
        return -1L;
    }

}
