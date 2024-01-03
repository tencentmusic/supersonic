package com.tencent.supersonic.headless.server.pojo;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;

import java.util.List;

@Data
public class Database extends RecordInfo {


    private Long id;

    private Long domainId;

    private String name;

    private String description;

    private String version;

    /**
     * mysql,clickhouse
     */
    private String type;

    private ConnectInfo connectInfo;

    private List<String> admins = Lists.newArrayList();

    private List<String> viewers = Lists.newArrayList();

}
