package com.tencent.supersonic.headless.server.utils;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import com.tencent.supersonic.headless.server.persistence.dataobject.DatabaseDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Arrays;

public class DatabaseConverter {

    public static DatabaseResp convert(DatabaseReq databaseReq) {
        DatabaseResp database = new DatabaseResp();
        BeanUtils.copyProperties(databaseReq, database);
        return database;
    }

    public static DatabaseDO convert(DatabaseReq databaseReq, DatabaseDO databaseDO) {
        BeanUtils.copyProperties(databaseReq, databaseDO);
        ConnectInfo connectInfo = getConnectInfo(databaseReq);
        databaseDO.setConfig(JSONObject.toJSONString(connectInfo));
        databaseDO.setAdmin(String.join(",", databaseReq.getAdmins()));
        databaseDO.setViewer(String.join(",", databaseReq.getViewers()));
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

    public static DatabaseDO convertDO(DatabaseReq databaseReq) {
        DatabaseDO databaseDO = new DatabaseDO();
        BeanUtils.copyProperties(databaseReq, databaseDO);
        ConnectInfo connectInfo = getConnectInfo(databaseReq);
        databaseDO.setConfig(JSONObject.toJSONString(connectInfo));
        databaseDO.setAdmin(String.join(",", databaseReq.getAdmins()));
        databaseDO.setViewer(String.join(",", databaseReq.getViewers()));
        return databaseDO;
    }

    public static DatabaseResp convertWithPassword(DatabaseDO databaseDO) {
        DatabaseResp databaseResp = convert(databaseDO);
        ConnectInfo connectInfo = JSONObject.parseObject(databaseDO.getConfig(), ConnectInfo.class);
        databaseResp.setPassword(connectInfo.getPassword());
        return databaseResp;
    }

    public static ConnectInfo getConnectInfo(DatabaseResp database) {
        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setUserName(database.getUsername());
        connectInfo.setPassword(database.passwordDecrypt());
        connectInfo.setUrl(database.getUrl());
        connectInfo.setDatabase(database.getDatabase());
        return connectInfo;
    }

    public static ConnectInfo getConnectInfo(DatabaseReq databaseReq) {
        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setUserName(databaseReq.getUsername());
        connectInfo.setPassword(databaseReq.getPassword());
        connectInfo.setUrl(databaseReq.getUrl());
        connectInfo.setDatabase(databaseReq.getDatabase());
        return connectInfo;
    }
}
