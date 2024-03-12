package com.tencent.supersonic.headless.api.pojo.request;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.pojo.Cache;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
@ToString
public class QueryMultiStructReq extends SemanticQueryReq {

    List<QueryStructReq> queryStructReqs;

    public String toCustomizedString() {
        return JSONObject.toJSONString(queryStructReqs);
    }

    public String generateCommandMd5() {
        return DigestUtils.md5Hex(this.toCustomizedString());
    }

    public Long getViewId() {
        if (CollectionUtils.isEmpty(this.getQueryStructReqs())) {
            return null;
        }
        return this.getQueryStructReqs().get(0).getDataSetId();
    }

    public Cache getCacheInfo() {
        if (CollectionUtils.isEmpty(this.getQueryStructReqs())) {
            return getCacheInfo();
        }
        return this.getQueryStructReqs().get(0).getCacheInfo();
    }

}
