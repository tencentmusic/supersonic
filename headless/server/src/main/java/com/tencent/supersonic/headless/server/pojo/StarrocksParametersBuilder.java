package com.tencent.supersonic.headless.server.pojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class StarrocksParametersBuilder extends DefaultParametersBuilder {

    @Override
    public List<DatabaseParameter> build() {
        List<DatabaseParameter> databaseParameters = new ArrayList<>();
        DatabaseParameter host = new DatabaseParameter();
        host.setComment("JDBC连接");
        host.setValue("jdbc:mysql://localhost:9030/dbname");
        host.setName("url");
        host.setPlaceholder("请输入JDBC连接串");
        databaseParameters.add(host);

        DatabaseParameter userName = new DatabaseParameter();
        userName.setComment("用户名");
        userName.setName("username");
        userName.setPlaceholder("请输入用户名");
        databaseParameters.add(userName);

        DatabaseParameter password = new DatabaseParameter();
        password.setComment("密码");
        password.setName("password");
        password.setPlaceholder("请输入密码");
        password.setRequire(false);
        databaseParameters.add(password);

        return databaseParameters;
    }
}
