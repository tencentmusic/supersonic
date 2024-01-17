package com.tencent.supersonic.headless.server.pojo;


import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class H2ParametersBuilder implements DbParametersBuilder {

    @Override
    public List<DatabaseParameter> build() {
        List<DatabaseParameter> databaseParameters = new ArrayList<>();
        DatabaseParameter host = new DatabaseParameter();
        host.setComment("链接");
        host.setName("url");
        host.setPlaceholder("请输入链接");
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
        databaseParameters.add(password);

        DatabaseParameter database = new DatabaseParameter();
        database.setComment("数据库名称");
        database.setName("database");
        database.setPlaceholder("请输入数据库名称");
        database.setRequire(false);
        databaseParameters.add(database);
        return databaseParameters;
    }
}
