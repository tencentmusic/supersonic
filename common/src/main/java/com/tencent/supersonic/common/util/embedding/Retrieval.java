package com.tencent.supersonic.common.util.embedding;

import lombok.Data;

import java.util.Map;

@Data
public class Retrieval {

    private Long id;

    private double distance;

    private String query;

    private Map<String, String> metadata;

}
