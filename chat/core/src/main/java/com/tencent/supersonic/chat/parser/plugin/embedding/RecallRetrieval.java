package com.tencent.supersonic.chat.parser.plugin.embedding;


import lombok.Data;

@Data
public class RecallRetrieval {

    private String id;

    private String distance;

    private String presetQuery;

    private String presetId;

    private String query;

}
