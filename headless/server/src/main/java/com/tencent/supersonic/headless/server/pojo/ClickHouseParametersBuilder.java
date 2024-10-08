package com.tencent.supersonic.headless.server.pojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ClickHouseParametersBuilder extends DefaultParametersBuilder {

    @Override
    public List<DatabaseParameter> build() {
        List<DatabaseParameter> databaseParameters = super.build();
        DatabaseParameter database = new DatabaseParameter();
        database.setComment("数据库名称");
        database.setName("database");
        database.setPlaceholder("请输入数据库名称");
        database.setRequire(false);
        databaseParameters.add(database);
        return databaseParameters;
    }
}
