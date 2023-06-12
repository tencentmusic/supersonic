package com.tencent.supersonic.semantic.core.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.core.response.DatabaseResp;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatabaseDO;
import com.tencent.supersonic.semantic.core.domain.pojo.ConnectInfo;
import com.tencent.supersonic.semantic.core.domain.pojo.Database;
import java.util.Date;
import org.springframework.beans.BeanUtils;

public class DatabaseConverter {


    public static Database convert(DatabaseReq databaseReq, User user) {
        Database database = new Database();
        BeanUtils.copyProperties(databaseReq, database);
        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setUserName(databaseReq.getUsername());
        connectInfo.setPassword(databaseReq.getPassword());
        connectInfo.setUrl(databaseReq.getUrl());
        database.setConnectInfo(connectInfo);
        database.setCreatedAt(new Date());
        database.setCreatedBy(user.getName());
        database.setUpdatedAt(new Date());
        database.setUpdatedBy(user.getName());
        return database;
    }

    public static DatabaseDO convert(Database database, DatabaseDO databaseDO) {
        database.setId(databaseDO.getId());
        BeanUtils.copyProperties(database, databaseDO);
        databaseDO.setConfig(JSONObject.toJSONString(database.getConnectInfo()));
        return databaseDO;
    }


    public static DatabaseDO convert(Database database) {
        DatabaseDO databaseDO = new DatabaseDO();
        BeanUtils.copyProperties(database, databaseDO);
        databaseDO.setConfig(JSONObject.toJSONString(database.getConnectInfo()));
        return databaseDO;
    }


    public static DatabaseResp convert(DatabaseDO databaseDO) {
        DatabaseResp databaseResp = new DatabaseResp();
        BeanUtils.copyProperties(databaseDO, databaseResp);
        ConnectInfo connectInfo = JSONObject.parseObject(databaseDO.getConfig(), ConnectInfo.class);
        databaseResp.setUrl(connectInfo.getUrl());
        databaseResp.setPassword(connectInfo.getPassword());
        databaseResp.setUsername(connectInfo.getUserName());
        return databaseResp;
    }


}
