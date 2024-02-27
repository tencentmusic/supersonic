package com.tencent.supersonic.headless.server.utils;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DatabaseDO;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import com.tencent.supersonic.headless.core.pojo.Database;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import java.util.Arrays;

public class DatabaseConverter {

    public static Database convert(DatabaseResp databaseResp) {
        Database database = new Database();
        BeanUtils.copyProperties(databaseResp, database);
        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setUserName(databaseResp.getUsername());
        connectInfo.setPassword(databaseResp.getPassword());
        connectInfo.setUrl(databaseResp.getUrl());
        connectInfo.setDatabase(databaseResp.getDatabase());
        database.setConnectInfo(connectInfo);
        database.setVersion(databaseResp.getVersion());
        return database;
    }

    public static Database convert(DatabaseReq databaseReq) {
        Database database = new Database();
        BeanUtils.copyProperties(databaseReq, database);
        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setUserName(databaseReq.getUsername());
        connectInfo.setPassword(databaseReq.getPassword());
        connectInfo.setUrl(databaseReq.getConnectUrl());
        connectInfo.setDatabase(databaseReq.getDatabase());
        database.setConnectInfo(connectInfo);
        database.setVersion(databaseReq.getVersion());
        return database;
    }

    public static DatabaseDO convert(Database database, DatabaseDO databaseDO) {
        database.setId(databaseDO.getId());
        database.setCreatedBy(databaseDO.getCreatedBy());
        database.setCreatedAt(databaseDO.getCreatedAt());
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
        databaseResp.setUsername(connectInfo.getUserName());
        databaseResp.setDatabase(connectInfo.getDatabase());
        if (StringUtils.isNotBlank(databaseDO.getAdmin())) {
            databaseResp.setAdmins(Arrays.asList(databaseDO.getAdmin().split(",")));
        }
        if (StringUtils.isNotBlank(databaseDO.getViewer())) {
            databaseResp.setViewers(Arrays.asList(databaseDO.getViewer().split(",")));
        }
        return databaseResp;
    }

    public static DatabaseResp convertWithPassword(DatabaseDO databaseDO) {
        DatabaseResp databaseResp = convert(databaseDO);
        ConnectInfo connectInfo = JSONObject.parseObject(databaseDO.getConfig(), ConnectInfo.class);
        databaseResp.setPassword(connectInfo.getPassword());
        return databaseResp;
    }

}
