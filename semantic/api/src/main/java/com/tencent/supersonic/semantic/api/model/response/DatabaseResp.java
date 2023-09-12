package com.tencent.supersonic.semantic.api.model.response;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatabaseResp extends RecordInfo {

    private Long id;

    private String name;

    private String description;

    private List<String> admins = Lists.newArrayList();

    private List<String> viewers = Lists.newArrayList();

    private String type;

    private String url;

    private String username;

    private String password;

    private String database;

    private String version;

    private boolean hasPermission = false;

    private boolean hasUsePermission = false;

    private boolean hasEditPermission = false;

    public String getHost() {
        Pattern p = Pattern.compile("jdbc:(?<db>\\w+):.*((//)|@)(?<host>.+):(?<port>\\d+).*");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group("host");
        }
        return "";
    }

    public String getPort() {
        Pattern p = Pattern.compile("jdbc:(?<db>\\w+):.*((//)|@)(?<host>.+):(?<port>\\d+).*");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group("port");
        }
        return "";
    }

}