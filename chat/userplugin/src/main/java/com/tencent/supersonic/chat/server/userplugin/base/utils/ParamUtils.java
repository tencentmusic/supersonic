package com.tencent.supersonic.chat.server.userplugin.base.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import java.util.stream.Collectors;

public class ParamUtils {
    public static Object dealOneParam(Object param, String dictKey) {
        if (param instanceof String) {
            String str = param.toString();
            if (str.startsWith("[") && str.endsWith("]")) { // 解决 和字的理解误差
                try {
                    JSONArray tmp = JSON.parseArray(str);
                    str = tmp.stream().map(e -> e.toString()).collect(Collectors.joining("和"));
                } catch (Exception e) {
                    return param;
                }
            }
            str = SegmentUtils.getTerm(str.toString().toLowerCase(), dictKey);
            return str;
        }
        return param;
    }


}
