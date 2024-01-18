package com.tencent.supersonic.headless.api.request;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.pojo.Cache;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.CollectionUtils;

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

    public List<Long> getModelIds() {
        if (CollectionUtils.isEmpty(this.getQueryStructReqs())) {
            return new ArrayList<>();
        }
        return this.getQueryStructReqs().get(0).getModelIds();
    }

    public Cache getCacheInfo() {
        if (CollectionUtils.isEmpty(this.getQueryStructReqs())) {
            return getCacheInfo();
        }
        return this.getQueryStructReqs().get(0).getCacheInfo();
    }

}
