package com.tencent.supersonic.semantic.api.core.request;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;


@Data
public class DatabaseReq {


    private Long id;

    private Long domainId;

    private String name;

    private String type;

    private String host;

    private String port;

    private String username;

    private String password;

    private String database;

    private String description;

    private String url;

    public String getUrl() {
        if (StringUtils.isNotBlank(url)) {
            return url;
        }
        return String.format("jdbc:%s://%s:%s", type, host, port);
    }
}