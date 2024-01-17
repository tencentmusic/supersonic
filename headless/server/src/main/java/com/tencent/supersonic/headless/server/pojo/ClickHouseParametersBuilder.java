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
        host.setName("host");
        host.setEnName("host");
        host.setComment("请输入host");
        databaseParameters.add(host);

        DatabaseParameter port = new DatabaseParameter();
        port.setName("port");
        port.setEnName("port");
        port.setComment("请输入端口号");
        databaseParameters.add(port);

        DatabaseParameter userName = new DatabaseParameter();
        userName.setName("用户名");
        userName.setEnName("username");
        databaseParameters.add(userName);

        DatabaseParameter password = new DatabaseParameter();
        password.setName("密码");
        password.setEnName("password");
        databaseParameters.add(password);

        DatabaseParameter database = new DatabaseParameter();
        database.setName("数据库名称");
        database.setEnName("database");
        databaseParameters.add(database);
        return databaseParameters;
    }
}
