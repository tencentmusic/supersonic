package com.tencent.supersonic.semantic.api.model.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.semantic.api.model.enums.DataTypeEnum;
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

    private String url;

    private List<String> admins = Lists.newArrayList();

    private List<String> viewers = Lists.newArrayList();

    public String getUrl() {
        if (type.equalsIgnoreCase(DataTypeEnum.H2.getFeature())) {
            return url;
        }
        String databaseUrl = database;
        if (StringUtils.isBlank(databaseUrl)) {
            databaseUrl = "";
        } else {
            databaseUrl = "/" + database;
        }
        if (type.equalsIgnoreCase(DataTypeEnum.MYSQL.getFeature())) {
            return String.format("jdbc:%s://%s:%s%s?sessionVariables=sql_mode='IGNORE_SPACE'&allowMultiQueries=true",
                    type, host, port, databaseUrl);
        }
        return String.format("jdbc:%s://%s:%s%s", type, host, port, databaseUrl);
    }
}