package com.tencent.supersonic.headless.server.pojo;


import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClickHouseParametersBuilder implements DbParametersBuilder {

    @Override
    public List<DatabaseParameter> build() {
        List<DatabaseParameter> databaseParameters = new ArrayList<>();
        DatabaseParameter host = new DatabaseParameter();
        host.setComment("host");
        host.setName("host");
        host.setPlaceholder("请输入host");
        databaseParameters.add(host);

        DatabaseParameter port = new DatabaseParameter();
        port.setComment("port");
        port.setName("port");
        port.setPlaceholder("请输入端口号");
        databaseParameters.add(port);

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
