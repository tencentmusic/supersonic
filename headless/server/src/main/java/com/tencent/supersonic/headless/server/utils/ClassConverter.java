package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.ClassReq;
import com.tencent.supersonic.headless.api.pojo.response.ClassResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ClassDO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: kanedai
 * @date: 2024/4/23
 */
public class ClassConverter {

    public static ClassDO convert(ClassReq classReq) {
        ClassDO classDO = new ClassDO();
        BeanUtils.copyProperties(classReq, classDO);
        classDO.setType(classReq.getTypeEnum().name());
        List<Long> itemIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(classReq.getItemIds())) {
            itemIds = classReq.getItemIds();
        }
        classDO.setItemIds(JsonUtil.toString(itemIds));
        
        return classDO;
    }

    public static ClassResp convert2Resp(ClassDO classDOById) {
        return null;
    }
}