package com.tencent.supersonic.headless.core.pojo;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Database extends RecordInfo {

    private Long id;

    private Long domainId;

    private String name;

    private String description;

    private String version;

    private String url;

    private String username;

    private String password;

    private String database;

    private String schema;
    /**
     * mysql,clickhouse
     */
    private String type;

    private ConnectInfo connectInfo;

    private List<String> admins = Lists.newArrayList();

    private List<String> viewers = Lists.newArrayList();

}
