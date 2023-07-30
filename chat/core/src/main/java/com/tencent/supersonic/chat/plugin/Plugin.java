package com.tencent.supersonic.chat.plugin;


import com.tencent.supersonic.chat.parser.ParseMode;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class Plugin extends RecordInfo {

    private Long id;

    //plugin type WEB_PAGE WEB_SERVICE
    private String type;

    private List<Long> domainList;

    //description, for parsing
    private String pattern;

    //parse
    private ParseMode parseMode;

    private String name;

    //config for different plugin type
    private String config;

    public List<String> getPatterns() {
        return Stream.of(getPattern().split("\\|")).collect(Collectors.toList());
    }

}
