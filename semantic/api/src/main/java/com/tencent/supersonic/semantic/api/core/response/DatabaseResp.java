package com.tencent.supersonic.semantic.api.core.response;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatabaseResp {

    private Long id;

    private String name;

    private String type;

    private String url;

    private String username;

    private String password;

    private String database;

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