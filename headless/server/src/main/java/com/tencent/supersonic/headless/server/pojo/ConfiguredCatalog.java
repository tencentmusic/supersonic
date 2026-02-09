package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ConfiguredCatalog {
    private List<ConfiguredStream> streams;
}
