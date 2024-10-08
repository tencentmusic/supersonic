package com.tencent.supersonic.headless.server.utils;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.headless.core.utils.DataTransformUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DataTransformUtilsTest {

    @Test
    public void testTransform() {
        List<Map<String, Object>> inputData = new ArrayList<>();
        inputData.add(createMap("2023/10/11", "a", "b", "1"));
        inputData.add(createMap("2023/10/12", "a", "c", "2"));
        inputData.add(createMap("2023/10/13", "a", "b", "3"));
        inputData.add(createMap("2023/10/14", "a", "c", "4"));
        inputData.add(createMap("2023/10/15", "b", "b", "5"));
        List<String> groups = Lists.newArrayList("d1", "d2");
        String metric = "m1";
        List<Map<String, Object>> resultData =
                DataTransformUtils.transform(inputData, metric, groups, new DateConf());
        Assertions.assertEquals(3, resultData.size());
    }

    private static Map<String, Object> createMap(String sysImpDate, String d1, String d2,
            String m1) {
        Map<String, Object> map = new HashMap<>();
        map.put("sys_imp_date", sysImpDate);
        map.put("d1", d1);
        map.put("d2", d2);
        map.put("m1", m1);
        return map;
    }
}
