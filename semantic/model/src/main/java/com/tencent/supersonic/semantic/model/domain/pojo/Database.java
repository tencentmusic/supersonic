package com.tencent.supersonic.semantic.model.domain.pojo;


import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;

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


}
