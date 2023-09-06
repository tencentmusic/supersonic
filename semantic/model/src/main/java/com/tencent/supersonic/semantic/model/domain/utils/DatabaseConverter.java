package com.tencent.supersonic.semantic.model.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatabaseDO;
import com.tencent.supersonic.semantic.model.domain.pojo.ConnectInfo;
import com.tencent.supersonic.semantic.model.domain.pojo.Database;
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
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
        database.setVersion(databaseReq.getVersion());
        return database;
    }

    public static DatabaseDO convert(Database database, DatabaseDO databaseDO) {
        database.setId(databaseDO.getId());
        BeanUtils.copyProperties(database, databaseDO);
        databaseDO.setConfig(JSONObject.toJSONString(database.getConnectInfo()));
        databaseDO.setAdmin(String.join(",", database.getAdmins()));
        databaseDO.setViewer(String.join(",", database.getViewers()));
        return databaseDO;
    }


    public static DatabaseDO convert(Database database) {
        DatabaseDO databaseDO = new DatabaseDO();
        BeanUtils.copyProperties(database, databaseDO);
        databaseDO.setConfig(JSONObject.toJSONString(database.getConnectInfo()));
        databaseDO.setAdmin(String.join(",", database.getAdmins()));
        databaseDO.setViewer(String.join(",", database.getViewers()));
        return databaseDO;
    }


    public static DatabaseResp convert(DatabaseDO databaseDO) {
        DatabaseResp databaseResp = new DatabaseResp();
        BeanUtils.copyProperties(databaseDO, databaseResp);
        ConnectInfo connectInfo = JSONObject.parseObject(databaseDO.getConfig(), ConnectInfo.class);
        databaseResp.setUrl(connectInfo.getUrl());
        databaseResp.setPassword(connectInfo.getPassword());
        databaseResp.setUsername(connectInfo.getUserName());
        if (StringUtils.isNotBlank(databaseDO.getAdmin())) {
            databaseResp.setAdmins(Arrays.asList(databaseDO.getAdmin().split(",")));
        }
        if (StringUtils.isNotBlank(databaseDO.getViewer())) {
            databaseResp.setViewers(Arrays.asList(databaseDO.getViewer().split(",")));
        }
        return databaseResp;
    }


}
