package com.tencent.supersonic.semantic.core.domain.pojo;


import com.tencent.supersonic.common.util.RecordInfo;
import lombok.Data;

@Data
public class Database extends RecordInfo {


    private Long id;

    private Long domainId;

    private String name;

    private String description;

    /**
     * mysql,clickhouse
     */
    private String type;

    private ConnectInfo connectInfo;


}
