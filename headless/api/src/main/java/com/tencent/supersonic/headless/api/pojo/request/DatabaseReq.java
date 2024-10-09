package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;

import java.util.List;

@Data
public class DatabaseReq extends RecordInfo {

    private Long id;

    private String name;

    private String type;

    private String host;

    private String port;

    private String username;

    private String password;

    private String database;

    private String databaseType;

    private String version;

    private String description;

    private String schema;
    private String url;

    private List<String> admins = Lists.newArrayList();

    private List<String> viewers = Lists.newArrayList();
}
