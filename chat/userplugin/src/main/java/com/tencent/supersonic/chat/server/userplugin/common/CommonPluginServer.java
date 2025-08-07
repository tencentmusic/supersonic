package com.tencent.supersonic.chat.server.userplugin.common;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.server.userplugin.base.utils.ParamUtils;
import com.tencent.supersonic.common.pojo.QueryColumn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("commonPluginServer")
public class CommonPluginServer extends UserPluginServer {
    private void init() {}

    private List<QueryColumn> setColumn(JSONArray columns) {
        List<QueryColumn> cols = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            JSONObject col = columns.getJSONObject(i);
            QueryColumn qc = new QueryColumn(col.getString("bizName"), col.getString("type"));
            qc.setName(col.getString("name"));
            qc.setShowType(col.getString("showType"));
            cols.add(qc);
        }
        return cols;
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> param) {
        init();
        Map<String, Object> res = new HashMap<>();
        Map<String, Object> paramCopy = new HashMap<>(param);
        res.put("resultList", new ArrayList<>());
        res.put("columns", new ArrayList<>());
        for (Map.Entry<String, Object> entry : param.entrySet()) {
            String key = entry.getKey();
            if (key.contains("_SysDict_")) {
                String[] tmps = key.split("_SysDict_");
                if (tmps.length == 2) {
                    paramCopy.put(key, ParamUtils.dealOneParam(entry.getValue(), tmps[1]));
                }
            }
        }
        String url = param.get("url").toString();
        if (url != null && !url.startsWith("http")) {
            /*try {  // demo
                JSONObject httpRes = InterfaceServerUtils
                        .resToObj(InterfaceServerUtils.sendPost(url, param, null));
                if (httpRes.containsKey("errorMsg")) {
                    res.put("textResult", httpRes.get("errorMsg"));
                } else {
                    res.put("textResult", httpRes.get("textResult"));
                    res.put("resultList", httpRes.get("resultList"));
                    res.put("columns", setColumn(httpRes.getJSONArray("columns")));
                }
            } catch (Exception e) {
                res.put("textResult", "你的请求出现了异常，异常信息如下：" + e.getMessage());
            }*/
			res.put("textResult", "需开发做功能，请联系开发");
        }
        return res;
    }
}
