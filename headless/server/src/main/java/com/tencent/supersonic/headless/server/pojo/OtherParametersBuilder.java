package com.tencent.supersonic.headless.server.pojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OtherParametersBuilder extends DefaultParametersBuilder {

    @Override
    public List<DatabaseParameter> build() {
        List<DatabaseParameter> databaseParameters = new ArrayList<>();
        DatabaseParameter databaseTypeName = new DatabaseParameter();
        databaseTypeName.setComment("数据库类型名称");
        databaseTypeName.setName("databaseType");
        databaseTypeName.setPlaceholder("请输入数据库类型名称");
        databaseParameters.add(databaseTypeName);

        databaseParameters.addAll(super.build());
        return databaseParameters;
    }
}
