package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import java.util.List;


@Data
public class DatabaseReq {

    private Long id;

    private String name;

    private String type;

    private String host;

    private String port;

    private String username;

    private String password;

    private String database;

    private String version;

    private String description;

    private String schema;
    private String url;

    private List<String> admins = Lists.newArrayList();

    private List<String> viewers = Lists.newArrayList();

    public String getConnectUrl() {
        if (StringUtils.isNotBlank(url)) {
            return url;
        }
        String databaseUrl = database;
        if (StringUtils.isBlank(databaseUrl)) {
            databaseUrl = "";
        } else {
            databaseUrl = "/" + database;
        }
        if (type.equalsIgnoreCase(DataType.MYSQL.getFeature())) {
            return String.format("jdbc:%s://%s:%s%s?sessionVariables=sql_mode='IGNORE_SPACE'&allowMultiQueries=true",
                    type, host, port, databaseUrl);
        }
        return String.format("jdbc:%s://%s:%s%s", type, host, port, databaseUrl);
    }
}