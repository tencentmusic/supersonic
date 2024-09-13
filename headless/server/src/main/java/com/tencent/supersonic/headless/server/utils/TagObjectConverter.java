package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.TagObjectReq;
import com.tencent.supersonic.headless.api.pojo.response.TagObjectResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagObjectDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class TagObjectConverter {

    public static TagObjectDO convert(TagObjectReq tagObjectReq) {
        TagObjectDO tagObjectDO = new TagObjectDO();
        BeanUtils.copyProperties(tagObjectReq, tagObjectDO);
        tagObjectDO.setId(null);
        tagObjectDO.setExt(tagObjectReq.getExtJson());
        return tagObjectDO;
    }

    public static TagObjectResp convert2Resp(TagObjectDO tagObjectDO) {
        TagObjectResp tagObjectResp = new TagObjectResp();
        BeanUtils.copyProperties(tagObjectDO, tagObjectResp);
        if (StringUtils.isNotEmpty(tagObjectDO.getExt())) {
            tagObjectResp.setExt(JsonUtil.objectToMapString(tagObjectDO.getExt()));
        }
        return tagObjectResp;
    }

    public static List<TagObjectResp> convert2RespList(List<TagObjectDO> tagObjectDOList) {
        List<TagObjectResp> tagObjectRespList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(tagObjectDOList)) {
            tagObjectDOList.stream()
                    .forEach(tagObjectDO -> tagObjectRespList.add(convert2Resp(tagObjectDO)));
        }
        return tagObjectRespList;
    }
}
